package dev.pandabreeder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import java.util.*;
public final class PandaBreederClient implements ClientModInitializer {
    public static PandaBreederClient INSTANCE;
    public CollectionData data=new CollectionData();
    public Optimizer.Plan plan;
    public String message="Look at a panda and press R.", identity;
    public String profile="default";
    private final Set<UUID> loaded=new HashSet<>(), seen=new HashSet<>();
    private int ticks;
    private KeyBinding register,open,toggle;
    @Override public void onInitializeClient() {
        INSTANCE=this;
        var category=KeyBinding.Category.create(Identifier.of("pandabreeder","pandas"));
        open=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pandabreeder.open",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_P,category));
        register=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pandabreeder.register",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_R,category));
        toggle=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pandabreeder.overlays",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_O,category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            updateContext(client);
            if(client.world==null||client.player==null) return;
            while(register.wasPressed()) registerTarget();
            while(open.wasPressed()) openScreen();
            while(toggle.wasPressed()) { data.overlays=!data.overlays; save(); }
            if(++ticks%20==0) refresh(client);
        });
    }
    public void openScreen() { notifyPlayer("Panda collection: "+data.pandas.size()+" registered."); }
    private void updateContext(MinecraftClient client) {
        String next=null;
        if(client.world!=null && client.player!=null) {
            String server=client.getServer()!=null?"local:"+client.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize():
                client.getCurrentServerEntry()!=null?"server:"+client.getCurrentServerEntry().address.toLowerCase(Locale.ROOT):"unknown";
            next=server+"|"+client.world.getRegistryKey().getValue()+"|"+profile;
        }
        if(Objects.equals(next,identity)) return;
        save(); identity=next; data=new CollectionData(); loaded.clear(); seen.clear(); plan=null;
        if(next!=null) load();
    }
    public void changeProfile(String value) {
        if(value.isBlank()||value.length()>64) { notifyPlayer("World profile must be 1–64 characters."); return; }
        profile=value.trim(); updateContext(MinecraftClient.getInstance());
    }
    public void load() {}
    public void save() {}
    public PandaEntity target() {
        return MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof PandaEntity p?p:null;
    }
    public void registerTarget() {
        var panda=target(); if(panda==null||!panda.isAlive()) { notifyPlayer("Look directly at a living panda."); return; }
        register(panda); save(); recompute();
    }
    public CollectionData.Panda register(PandaEntity entity) {
        var p=data.find(entity.getUuid());
        if(p==null) { p=new CollectionData.Panda(); p.uuid=entity.getUuid(); p.id=String.format(Locale.ROOT,"P%02d",data.nextId++); data.pandas.add(p); }
        snapshot(p,entity); loaded.add(p.uuid); notifyPlayer(p.id+" registered: "+p.main+" / "+p.hidden); return p;
    }
    private boolean snapshot(CollectionData.Panda p,PandaEntity entity) {
        var main=Genetics.Gene.valueOf(entity.getMainGene().name()); var hidden=Genetics.Gene.valueOf(entity.getHiddenGene().name());
        boolean changed=p.main!=main||p.hidden!=hidden||p.baby!=entity.isBaby()||p.dead!=!entity.isAlive();
        p.main=main; p.hidden=hidden; p.evidence="Synchronized"; p.uncertainty=null;
        p.baby=entity.isBaby(); p.dead=!entity.isAlive(); p.personality=entity.getProductGene().asString();
        p.dimension=entity.getEntityWorld().getRegistryKey().getValue().toString();
        p.x=entity.getX(); p.y=entity.getY(); p.z=entity.getZ(); p.lastSeen=System.currentTimeMillis(); return changed;
    }
    private void refresh(MinecraftClient client) {
        var previous=new HashSet<>(loaded); loaded.clear(); boolean changed=false;
        for(var entity:client.world.getEntities()) if(entity instanceof PandaEntity panda) {
            var p=data.find(panda.getUuid());
            if(p==null && data.autoBabies && !seen.contains(panda.getUuid()) && panda.isBaby()) {
                boolean near=data.pandas.stream().anyMatch(a->!a.baby&&a.dimension.equals(panda.getEntityWorld().getRegistryKey().getValue().toString())&&panda.squaredDistanceTo(a.x,a.y,a.z)<256);
                if(near) { p=register(panda); changed=true; notifyPlayer(p.id+": nearby baby registered; confirm parents manually."); }
            }
            seen.add(panda.getUuid());
            if(p!=null) { changed|=snapshot(p,panda); loaded.add(p.uuid); }
        }
        if(changed||!previous.equals(loaded)) recompute();
        if(changed||ticks%200==0) save();
    }
    public boolean isLoaded(CollectionData.Panda p) { return loaded.contains(p.uuid); }
    public void recompute() {
        plan=new Optimizer().recommend(data.pandas.stream().filter(p->!p.dead&&!p.baby&&isLoaded(p)&&p.genotype()!=null)
            .map(p->new Optimizer.Panda(p.id,p.genotype())).toList());
    }
    public void remove(CollectionData.Panda p) { data.remove(p); save(); recompute(); }
    public boolean recommended(CollectionData.Panda p) { return plan!=null&&(plan.first().equals(p.id)||plan.second().equals(p.id)); }
    public String status(CollectionData.Panda p) { return p.dead?"Observed dead":!isLoaded(p)?"Unloaded; last seen data":p.baby?"Baby; wait for growth":"Adult; check cooldown"; }
    public void notifyPlayer(String text) {
        message=text; var player=MinecraftClient.getInstance().player; if(player!=null) player.sendMessage(Text.literal(text),true);
    }
}
