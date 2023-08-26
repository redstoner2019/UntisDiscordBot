package me.redstoner2019;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bytedream.untis4j.LoginException;
import org.bytedream.untis4j.Session;
import org.bytedream.untis4j.responseObjects.Classes;
import org.bytedream.untis4j.responseObjects.Timetable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.*;
import java.awt.Color;
import java.util.*;

public class Main extends ListenerAdapter {

    public static TextChannel chatChannel;
    public static HashMap<Integer, Stunde> stundeHashMap = new HashMap<>();
    public static String className = "FA-23B";
    public static String password = "hnbk_KB_2023";
    public static String schoolName = "Nixdorf_BK_Essen";
    public static HashMap<String,Integer> stundenOffsets = new HashMap<>();
    public static Session session;
    public static String messageID = "";
    public static boolean reactionReady = false;
    public static JDA jda;
    public static String pingRoleID = "";
    public static List<Message> messagesPings = new ArrayList<>();
    public static boolean stundenEntfallen = false;

    public static LocalDate getDate(){
        return LocalDate.now(ZoneOffset.systemDefault());
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            session = Session.login(className,password, "https://mese.webuntis.com", schoolName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String token = "token";

        stundenOffsets.put("07:30",0);
        stundenOffsets.put("08:15",1);
        stundenOffsets.put("09:15",2);
        stundenOffsets.put("10:00",3);

        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.awaitReady();
        jda.addEventListener(new Main());

        chatChannel = jda.getTextChannels().get(0);

        if(Objects.equals(pingRoleID, "")){
            for(Role role : chatChannel.getGuild().getRoles()){
                if(role.getName().equals("Entfall Ping")){
                    pingRoleID = role.getId();
                    break;
                }
            }
        }
        if(Objects.equals(pingRoleID, "")){
            chatChannel.getGuild().createRole().setColor(Color.RED).setName("Entfall Ping").setPermissions(0L).complete();
        }

        assert chatChannel != null;
        List<Message> messages = chatChannel.getHistory().retrievePast(50).complete();
        for(Message msg : messages){
            if(msg.getAuthor().getEffectiveName().equals("UntisBot")){
                msg.delete().queue();
            }
        }
        sendMessage("React for Ping role");
        messageID = sendMessage("Timetable Message");
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    try{
                        refreshHours();
                        refreshEmbeds(messageID);
                        List<Integer> times = List.of(6,7);
                        if(times.contains(LocalTime.now(Clock.systemDefaultZone()).getHour())){
                            if(LocalTime.now(Clock.systemDefaultZone()).getMinute() == 0){
                                for(Message msg : messagesPings){
                                    try{
                                        if(msg.getAuthor().getEffectiveName().equals("UntisBot")){
                                            msg.delete().queue();
                                        }
                                    }catch (Exception ignored){}
                                }
                                messagesPings.clear();
                                if(stundenEntfallen){
                                    messagesPings.add(chatChannel.sendMessage("<@&" + pingRoleID + "> Es Entfallen Stunden!").complete());
                                }
                            }
                        }
                    } catch (Exception ignored){}
                    Thread.sleep(60000);
                } catch (InterruptedException ignored) {}
            }
        });
        t.start();

        reactionReady = true;

        chatChannel.getGuild().updateCommands().addCommands(Commands.slash("setup", "Setup UntisBot")).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("setup")){
            chatChannel = event.getChannel().asTextChannel();
            List<Message> messages = chatChannel.getHistory().retrievePast(50).complete();
            for(Message msg : messages){
                if(msg.getAuthor().getEffectiveName().equals("UntisBot")){
                    msg.delete().queue();
                }
            }
            sendMessage("React for Ping role");
            messageID = sendMessage("Timetable Message");
            refreshEmbeds(messageID);
            event.reply("Setup done").setEphemeral(true).queue();
        }
    }

    public static String sendMessage(String content){
        content = content.replaceAll("_","\\_");
        if(chatChannel == null){
            return null;
        }
        Message msg = chatChannel.sendMessage(content).complete();
        return msg.getId();
    }

    public static void refreshEmbeds(String id){
        List<MessageEmbed> embeds = new ArrayList<>();
        EmbedBuilder eb = new EmbedBuilder();
        if(stundeHashMap.size() != 0){
            for(int i = 0; i <12;i++){
                eb.clearFields();
                if(stundeHashMap.containsKey(i+1)){
                    Stunde s = stundeHashMap.get(i+1);
                    eb.setColor(s.getColor());
                    eb.addField(s.getStunde() + ". Stunde " + s.getName(), s.getTimes() + "\nInfo: " + s.getInfo() + "\nRaum: " + s.getRoom(), true);
                    embeds.add(eb.build());
                }
            }
        } else {
            eb.setColor(Color.RED);
            eb.addField("Kein Stundenplan", "Aktuell kein Stundenplan verfügbar!", true);
            embeds.add(eb.build());
        }
        try{
            chatChannel.editMessageById(id,"# Aktueller Stundenplan\nAktualisiert: " + "<t:" + (System.currentTimeMillis()/1000) + ":R>\nDatum: " + getDate().getDayOfWeek() + ", " + LocalDate.now(ZoneOffset.systemDefault())).setEmbeds(embeds).queue();
        }catch (Exception ignored){}
    }

    public static void refreshHours(){
        try {
            Classes classes = session.getClasses();
            int id = 0;
            for (Classes.ClassObject classObject : classes.searchByName(className)) {
                id = classObject.getId();
            }
            session.useCache(false);
            Timetable timetable = session.getTimetableFromClassId(getDate(), getDate(), id);
            stundeHashMap.clear();
            timetable.sortByStartTime();
            if(timetable.getStartTimes().size() != 0){
                int hourOffset = stundenOffsets.getOrDefault(timetable.getStartTimes().get(0).toString(),0);
                int stunde = 0;
                String letzteUhrzeit = "";
                for (int i = 0; i < timetable.getSubjects().size(); i++) {
                    Stunde s = new Stunde("",Color.RED,"","Findet Nicht Statt","","");

                    try{
                        s.setName(timetable.get(i).getSubjects().getLongNames().get(0));
                    }catch (Exception ignored){}

                    try{
                        s.setTimes(timetable.getStartTimes().get(i) + " - " + timetable.getEndTimes().get(i));
                    }catch (Exception ignored){}

                    try{
                        s.setTeacher(String.valueOf(timetable.getTeachers().toString()));
                    }catch (Exception ignored){}

                    try{
                        s.setRoom(timetable.getRooms().get(i).getName() + " (" + timetable.getRooms().get(i).getBuilding() + ")");
                    }catch (Exception ignored){}

                    if(letzteUhrzeit.equals(timetable.getStartTimes().get(i) + "")) {
                        letzteUhrzeit = timetable.getStartTimes().get(i) + "";
                    } else {
                        letzteUhrzeit = timetable.getStartTimes().get(i) + "";
                        stunde++;
                    }

                    s.setStunde(stunde + hourOffset);

                    s.setInfo(timetable.getActivityTypes().get(i));

                    s.setInfo(String.valueOf(timetable.getLessonCodes().get(i)));

                    if(s.getInfo().equals("IRREGULAR")){
                        s.setColor(Color.ORANGE);
                        stundenEntfallen = true;
                    }
                    if(s.getInfo().equals("CANCELLED")){
                        s.setColor(Color.RED);
                        stundenEntfallen = true;
                    }
                    if(s.getInfo().equals("REGULAR")){
                        s.setColor(Color.GREEN);
                    }

                    try{
                        boolean cancelled = false;
                        for(int i0 = 0; i0<timetable.get(i).getSubjects().getActiveTypes().size();i0++){
                            if(!cancelled){
                                cancelled = timetable.get(i).getSubjects().get(i0).isActive();
                            }
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    long start = localTimeToLong(timetable.getStartTimes().get(i));
                    long end = localTimeToLong(timetable.getEndTimes().get(i));
                    long now = localTimeToLong(LocalTime.now());
                    if(now <= start && now >= (start-300)){
                        s.setColor(Color.CYAN);
                    }else if(now <= end && now > start){
                        s.setColor(Color.BLUE);
                    }
                    stundeHashMap.put(i+1,s);
                }
            }

        } catch (LoginException e) {
            System.out.println("Failed to login: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long localTimeToLong(LocalTime time){
        return (time.getHour()*60*60) + (time.getMinute()*60) + time.getSecond();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(!reactionReady || Objects.requireNonNull(event.getUser()).getName().equals("UntisBot")){
            return;
        }
        if(event.getEmoji().equals(Emoji.fromUnicode("\uD83D\uDC4D"))){
            if(Objects.equals(pingRoleID, "")){
                for(Role role : chatChannel.getGuild().getRoles()){
                    if(role.getName().equals("Entfall Ping")){
                        pingRoleID = role.getId();
                        break;
                    }
                }
            }
            Role role;
            if(Objects.equals(pingRoleID, "")){
                role = chatChannel.getGuild().createRole().setColor(Color.RED).setName("Entfall Ping").setPermissions(0L).complete();
                if(Objects.requireNonNull(event.getMember()).getRoles().contains(role)){
                    event.getGuild().removeRoleFromMember(event.getUser(),role).queue();
                    System.out.println("Removing");
                } else {
                    event.getGuild().addRoleToMember(event.getUser(),role).queue();
                    System.out.println("Adding");
                }
            } else {
                role = chatChannel.getGuild().getRoleById(pingRoleID);
                if(role != null){
                    if(Objects.requireNonNull(event.getMember()).getRoles().contains(role)){
                        event.getGuild().removeRoleFromMember(event.getUser(),role).queue();
                        System.out.println("Removing");
                    } else {
                        event.getGuild().addRoleToMember(event.getUser(),role).queue();
                        System.out.println("Adding");
                    }
                }
            }
        }
        event.getReaction().removeReaction(event.getUser()).complete();
    }
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getMessage().getContentDisplay().equals("React for Ping role")){
            event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4D")).complete();
        }
    }
}