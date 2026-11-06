package dev.pandabreeder;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.random.Random;
import java.util.*;

/** Only the test fixture writes server genes. Never included in the distributable mod. */
public final class PandaBreederGameTest implements FabricClientGameTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksDownload();
            var ids=world.getServer().computeOnServer(server -> {
                var result=new ArrayList<UUID>(); var sw=server.getOverworld();var player=server.getPlayerManager().getPlayerList().getFirst();
                var genes=new PandaEntity.Gene[][]{{PandaEntity.Gene.NORMAL,PandaEntity.Gene.BROWN},{PandaEntity.Gene.BROWN,PandaEntity.Gene.BROWN},{PandaEntity.Gene.NORMAL,PandaEntity.Gene.NORMAL}};
                for(int i=0;i<3;i++) {
                    var p=EntityType.PANDA.create(sw,SpawnReason.COMMAND);check(p!=null,"create panda");
                    p.setPosition(player.getX()+i*3+1,player.getY(),player.getZ()+3);p.setAiDisabled(true);
                    p.setMainGene(genes[i][0]);p.setHiddenGene(genes[i][1]);if(i==2)p.setBreedingAge(-24000);
                    sw.spawnEntity(p);result.add(p.getUuid());
                }
                // Exhaustively verify every output of the vanilla mutation random selector.
                for(int i=0;i<16;i++) {
                    final int output=i;
                    Random scripted=(Random)java.lang.reflect.Proxy.newProxyInstance(Random.class.getClassLoader(),new Class<?>[]{Random.class},(proxy,method,args)->{
                        if(method.getName().equals("nextInt")&&args.length==1){check((int)args[0]==16,"mutation uses 16 outcomes");return output;}
                        throw new AssertionError("Unexpected random operation: "+method);
                    });
                    var actual=PandaEntity.Gene.createRandom(scripted);
                    var expected=i==0?PandaEntity.Gene.LAZY:i==1?PandaEntity.Gene.WORRIED:i==2?PandaEntity.Gene.PLAYFUL:i==4?PandaEntity.Gene.AGGRESSIVE:i<9?PandaEntity.Gene.WEAK:i<11?PandaEntity.Gene.BROWN:PandaEntity.Gene.NORMAL;
                    check(actual==expected,"vanilla mutation selector "+i);
                }
                // Check the complete engine distribution against real vanilla inheritance samples.
                var a=(PandaEntity)sw.getEntity(result.get(0));var b=(PandaEntity)sw.getEntity(result.get(1));var child=EntityType.PANDA.create(sw,SpawnReason.COMMAND);
                var expected=Genetics.offspring(Genetics.known(Genetics.Gene.NORMAL,Genetics.Gene.BROWN),Genetics.known(Genetics.Gene.BROWN,Genetics.Gene.BROWN));
                int[] counts=new int[49];child.getRandom().setSeed(983701L);
                for(int i=0;i<100000;i++){child.initGenes(a,b);counts[child.getMainGene().ordinal()*7+child.getHiddenGene().ordinal()]++;}
                for(var e:expected.entrySet()) {
                    int i=e.getKey().main().ordinal()*7+e.getKey().hidden().ordinal();double probability=e.getValue();
                    check(Math.abs(counts[i]/100000.0-probability)<7*Math.sqrt(probability*(1-probability)/100000)+0.0001,"inheritance parity "+e.getKey());
                }
                return result;
            });
            context.waitTicks(40);
            context.runOnClient(client->{
                var mod=PandaBreederClient.INSTANCE;check(mod.data.pandas.isEmpty(),"isolated test world");
                for(var id:ids) {
                    PandaEntity found=null;for(var e:client.world.getEntities())if(e.getUuid().equals(id))found=(PandaEntity)e;
                    check(found!=null,"panda synchronized");client.crosshairTarget=new EntityHitResult(found);mod.registerTarget();mod.registerTarget();
                }
                check(mod.data.pandas.size()==3,"no duplicate registration");
                check(mod.data.find(ids.get(0)).hidden==Genetics.Gene.BROWN,"hidden gene received");
                check(mod.data.find(ids.get(2)).main==Genetics.Gene.NORMAL&&mod.data.find(ids.get(2)).hidden==Genetics.Gene.NORMAL,"default normal tracked genes valid");
                check(mod.data.find(ids.get(2)).baby,"baby tracked");check(mod.plan!=null,"adult strategy available");
                PandaEntity labeled=null;for(var e:client.world.getEntities())if(e.getUuid().equals(ids.get(0)))labeled=(PandaEntity)e;
                var renderer=client.getEntityRenderDispatcher().getRenderer(labeled);
                check(renderer.getAndUpdateRenderState(labeled,0).displayName.getString().contains("P01"),"overlay render state");
                mod.data.overlays=false;check(renderer.getAndUpdateRenderState(labeled,0).displayName==null,"overlay toggle respects original unnamed entity");mod.data.overlays=true;
                mod.openScreen();
            });
            context.waitTicks(5);context.takeScreenshot("panda-collection");
            context.clickScreenButton("P01");context.clickScreenButton("Parent A");
            context.clickScreenButton("P02");context.clickScreenButton("Parent B");
            context.clickScreenButton("P03");context.clickScreenButton("Assign baby");
            context.runOnClient(c->check(PandaBreederClient.INSTANCE.data.find(ids.get(2)).parentA.equals(ids.get(0)),"UI parent assignment"));
            context.clickScreenButton("Calculator");context.waitTicks(3);context.takeScreenshot("panda-calculator");
            context.clickScreenButton("Strategy");context.waitTicks(3);context.takeScreenshot("panda-strategy");
            context.clickScreenButton("Settings");context.waitTicks(3);context.takeScreenshot("panda-settings");
            context.clickScreenButton("Done");
            world.getServer().runOnServer(server->{((PandaEntity)server.getOverworld().getEntity(ids.get(0))).setHiddenGene(PandaEntity.Gene.LAZY);});
            context.waitTicks(40);
            context.runOnClient(client->{
                var mod=PandaBreederClient.INSTANCE;check(mod.data.find(ids.get(0)).hidden==Genetics.Gene.LAZY,"dirty metadata refresh");
                mod.save();mod.load();check(mod.data.pandas.size()==3,"persistence reload");check(mod.data.find(ids.get(2)).parentB.equals(ids.get(1)),"parentage persists");
                mod.changeProfile("integration-other");check(mod.data.pandas.isEmpty(),"profile isolation");mod.changeProfile("default");check(mod.data.pandas.size()==3,"profile restoration");
            });
            context.waitTicks(25);
            context.runOnClient(client->{client.player.setYaw(-25);client.player.setPitch(0);PandaBreederClient.INSTANCE.data.autoBabies=true;});
            var babyId=world.getServer().computeOnServer(server->{
                var sw=server.getOverworld();var adult=(PandaEntity)sw.getEntity(ids.get(0));var baby=EntityType.PANDA.create(sw,SpawnReason.COMMAND);
                baby.setPosition(adult.getX()+1,adult.getY(),adult.getZ()+1);baby.setBreedingAge(-24000);baby.setAiDisabled(true);sw.spawnEntity(baby);return baby.getUuid();
            });
            context.waitTicks(40);
            context.runOnClient(client->{var p=PandaBreederClient.INSTANCE.data.find(babyId);check(p!=null&&p.baby,"automatic nearby baby discovery");check(p.parentA==null&&p.parentB==null,"discovery never invents parentage");});
            context.takeScreenshot("panda-overlays");
            System.out.println("PANDABREEDER INTEGRATION PASS: mutation selector, vanilla inheritance parity, both synchronized genes, registration, GUI parentage, metadata refresh, persistence and profile isolation.");
        }
        context.waitTicks(5);context.runOnClient(c->check(PandaBreederClient.INSTANCE.data.pandas.isEmpty(),"disconnect clears active collection"));
    }
}
