package dev.pandabreeder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

public final class BreedingScreen extends Screen {
    private final PandaBreederClient mod;
    private int tab,offset;
    private UUID selected,first,second;
    private TextFieldWidget profile;
    private static final int WHITE=0xFFF1F4EC,GREEN=0xFF9DDE94,GREY=0xFFAFB8AD;
    public BreedingScreen(PandaBreederClient mod) { super(Text.literal("Panda Breeder")); this.mod=mod; }
    private void button(String label,String hint,int x,int y,int w,Runnable action) {
        var b=ButtonWidget.builder(Text.literal(label),v->action.run()).dimensions(x,y,w,20).build();
        b.setTooltip(Tooltip.of(Text.literal(hint))); addDrawableChild(b);
    }
    private CollectionData.Panda find(UUID id) { return id==null?null:mod.data.find(id); }
    private String id(UUID uuid) { var p=find(uuid); return p==null?"—":p.id; }
    private int rows() { return Math.max(1,(height-126)/34); }
    @Override protected void init() {
        clearChildren(); int tw=(width-20)/4;
        String[] tabs={"Collection","Calculator","Strategy","Settings"};
        for(int i=0;i<4;i++) { final int index=i; button(tabs[i],"Open "+tabs[i],10+i*tw,26,tw-3,()->{tab=index;offset=0;init();}); }
        button("Done","Return to game",width-65,height-25,55,this::close);
        if(tab==0) {
            offset=Math.max(0,Math.min(offset,Math.max(0,mod.data.pandas.size()-rows())));
            for(int i=0;i<rows()&&offset+i<mod.data.pandas.size();i++) {
                var p=mod.data.pandas.get(offset+i);
                button(p.id+(p.uuid.equals(selected)?" *":""),"Select for inspection or assigning parents",10,54+i*34,48,()->{selected=p.uuid;init();});
            }
            button("↑","Scroll up",width-30,52,20,()->{offset=Math.max(0,offset-1);init();});
            button("↓","Scroll down",width-30,76,20,()->{offset++;init();});
            button("Inspect","Inspect selected panda",10,height-69,62,()->{if(find(selected)!=null){tab=4;offset=0;init();}});
            button("Parent A","Use selected panda as first parent",76,height-69,70,()->{first=selected;init();});
            button("Parent B","Use selected panda as second parent",150,height-69,70,()->{second=selected;init();});
            button("Assign baby","Assign A and B to selected offspring; manual confirmation",224,height-69,85,()->{
                try {mod.data.parentage(find(selected),find(first),find(second));mod.save();mod.recompute();mod.notifyPlayer("Parentage recorded (manually confirmed).");}
                catch(IllegalArgumentException e){mod.notifyPlayer(e.getMessage());} init();
            });
        } else if(tab==1) {
            button("Cycle A: "+id(first),"Choose first panda; includes saved and baby records",10,52,(width-24)/2,()->{first=cycle(first);init();});
            button("Cycle B: "+id(second),"Choose a different second panda",width/2+2,52,(width-24)/2,()->{second=cycle(second);init();});
            button("↑","Previous genotypes",width-30,106,20,()->{offset=Math.max(0,offset-1);});
            button("↓","Next genotypes",width-30,130,20,()->{offset=Math.min(48,offset+1);});
        } else if(tab==3) {
            button("Labels: "+mod.data.overlays,"Toggle all in-world panda labels (also O)",10,56,145,()->{mod.data.overlays=!mod.data.overlays;mod.save();init();});
            button("Auto babies: "+mod.data.autoBabies,"Register newly seen babies within 16 blocks of a tracked adult; never assumes parentage",160,56,150,()->{mod.data.autoBabies=!mod.data.autoBabies;mod.save();init();});
            profile=new TextFieldWidget(textRenderer,10,112,190,20,Text.literal("World profile")); profile.setMaxLength(64);profile.setText(mod.profile);addDrawableChild(profile);
            button("Switch profile","Separate worlds on the same server or after a reset",205,112,105,()->{mod.changeProfile(profile.getText());selected=first=second=null;init();});
        } else if(tab==4) {
            button("Unregister","Remove only the saved record; never harms the panda",10,height-52,100,()->{var p=find(selected);if(p!=null)mod.remove(p);selected=null;tab=0;init();});
        }
    }
    private UUID cycle(UUID current) {
        if(mod.data.pandas.isEmpty())return null;
        var p=find(current);int i=mod.data.pandas.indexOf(p);return mod.data.pandas.get((i+1)%mod.data.pandas.size()).uuid;
    }
    private void line(DrawContext c,String s,int y,int color) { c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(s,width-24),12,y,color); }
    private static String f(String fmt,Object...args){return String.format(Locale.ROOT,fmt,args);}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0xF018231D);c.fill(0,0,width,22,0xFF273C2D);
        line(c,"PANDA BREEDER  /  "+mod.data.pandas.size()+" pandas",7,GREEN);
        if(tab==0) {
            for(int i=0;i<rows()&&offset+i<mod.data.pandas.size();i++) {
                var p=mod.data.pandas.get(offset+i);int y=54+i*34;
                c.fill(62,y,width-34,y+30,mod.recommended(p)?0xFF304D32:0xFF26322B);
                String genes=p.genotype()==null?"Inferred / uncertain":p.main+" / "+p.hidden;
                c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(genes,width-104),66,y+3,WHITE);
                c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(mod.status(p)+" | brown copies: "+(p.genotype()==null?"?":p.genotype().brownCopies()),width-104),66,y+15,GREY);
            }
            line(c,"Parents: "+id(first)+" + "+id(second)+" | Selected offspring: "+id(selected),height-44,GREY);
        } else if(tab==1) calculator(c);
        else if(tab==2) strategy(c);
        else if(tab==3) {
            line(c,"World profile (use a distinct name after a server reset):",92,WHITE);
            line(c,"Collections are isolated by server/save, dimension and profile.",145,GREY);
            line(c,"P: open  |  R: register target  |  O: labels",165,GREEN);
            line(c,"Rebind all keys in Options → Controls → Key Binds.",179,GREY);
        } else if(tab==4) {
            var p=find(selected);
            if(p!=null) {
                String[] lines={p.id+"  "+mod.status(p),"Genes: "+p.main+" / "+p.hidden,"Evidence: "+p.evidence,
                    "Personality: "+p.personality,f("Last location: %.1f, %.1f, %.1f",p.x,p.y,p.z),"Dimension: "+p.dimension,
                    "Parents (manual): "+id(p.parentA)+" + "+id(p.parentB),"Recorded offspring: "+p.children.size(),"UUID: "+p.uuid};
                for(int i=0;i<lines.length;i++)if(54+i*15<height-60)line(c,lines[i],54+i*15,i==0?GREEN:WHITE);
            }
        }
        line(c,mod.message,height-13,GREY);
        super.render(c,mx,my,delta);
    }
    private void calculator(DrawContext c) {
        var a=find(first);var b=find(second);
        if(a==null||b==null||a==b){line(c,"Choose two different pandas.",82,GREY);return;}
        var distribution=Genetics.offspring(a.belief(),b.belief());
        line(c,f("Brown %.4f%% | One brown %.4f%% | None %.4f%%",100*Genetics.copies(distribution,2),100*Genetics.copies(distribution,1),100*Genetics.copies(distribution,0)),80,GREEN);
        line(c,f("Fixed-pair mean: %.2f attempts | %s",Genetics.expectedAttempts(distribution),a.genotype()!=null&&b.genotype()!=null?"Synchronized genes":"Inferred; independent parent beliefs"),94,GREY);
        var entries=new ArrayList<>(distribution.entrySet());
        int count=Math.max(1,(height-150)/12); offset=Math.min(offset,Math.max(0,49-count));
        for(int i=0;i<count&&offset+i<49;i++) {
            var e=entries.get(offset+i);line(c,f("%s / %s: %.5f%%",e.getKey().main(),e.getKey().hidden(),100*e.getValue()),112+i*12,WHITE);
        }
    }
    private void strategy(DrawContext c) {
        var p=mod.plan;
        if(p==null){line(c,"Register at least two loaded adult pandas with known genes.",56,GREY);return;}
        String[] lines={"Best breeding pair: "+p.first()+" + "+p.second(),f("Brown next offspring: %.4f%%",100*p.brownProbability()),
            f("Adaptive mean: %.2f attempts (model below)",p.expectedAttempts()),
            "Retain new brown-gene carriers for future breeding.","Zero-copy pandas become lower priority once carriers are available.",
            "Exact minimum attempts under these assumptions:","Retain offspring; wait for maturity; unlimited space and bamboo.",
            "Vanilla mutations; no deaths, travel cost or cooldown cost.","Only loaded adults enter the current recommendation.",
            "Check bamboo, readiness and server rules in game.","This minimizes breeding attempts, not elapsed time."};
        for(int i=0;i<lines.length;i++)if(56+i*14<height-34)line(c,lines[i],56+i*14,i<3?GREEN:GREY);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(tab==0||tab==1){offset=Math.max(0,offset+(vertical<0?1:-1));init();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean shouldPause(){return false;}
}
