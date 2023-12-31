package me.redstoner2019;

///setup schoolname:Nixdorf_BK_Essen password:hnbk_KB_2023 classname:FA-23B

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bytedream.untis4j.LoginException;
import org.bytedream.untis4j.Session;
import org.bytedream.untis4j.responseObjects.Classes;
import org.bytedream.untis4j.responseObjects.Timetable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.*;
import java.util.*;
import java.util.List;

public class Main extends ListenerAdapter {

    public static HashMap<Integer, Stunde> stundeHashMap = new HashMap<>();
    /*public static String className = "FA-23B";
    public static String password = "hnbk_KB_2023";
    public static String schoolName = "Nixdorf_BK_Essen";*/
    public static HashMap<String,Integer> stundenOffsets = new HashMap<>();
    public static Session session;
    public static boolean reactionReady = false;
    public static JDA jda;
    public static String pingRoleID = "";
    public static List<Message> messagesPings = new ArrayList<>();
    public static boolean stundenEntfallen = false;
    public static LocalTime letzteStunde;
    public static LocalDate refreshTime;
    public static int hourOffset = 2;
    public static List<ServerData> serverData = new ArrayList<>();

    public static LocalDate getDate(){
        return LocalDate.now(ZoneOffset.ofHours(hourOffset));
        //return LocalDate.of(2023,10,16);
    }
    public static LocalTime getTime(){
        return LocalTime.now(ZoneOffset.ofHours(hourOffset));
        //return LocalTime.of(8,0);
    }

    public static void main(String[] args) {
        letzteStunde = LocalTime.of(2,0);

        String token = "";

        token = args[0];

        stundenOffsets.put("07:30",0);
        stundenOffsets.put("08:15",1);
        stundenOffsets.put("09:15",2);
        stundenOffsets.put("10:00",3);

        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        jda.addEventListener(new Main());

        //chatChannel = jda.getTextChannelById("1145726265278611567");

        for(Guild g : jda.getGuilds()){
            if(Objects.equals(pingRoleID, "")){
                for(Role role : g.getRoles()){
                    if(role.getName().equals("Entfall Ping")){
                        pingRoleID = role.getId();
                        break;
                    }
                }
            }
        }

        for(Guild g : jda.getGuilds()){
            if(Objects.equals(pingRoleID, "")){
                g.createRole().setColor(Color.RED).setName("Entfall Ping").setPermissions(0L).complete();
            }
        }


        for(Guild g : jda.getGuilds()){
            for(GuildChannel channel : g.getChannels()){
                if(channel instanceof TextChannel){
                    TextChannel channel1 = (TextChannel) channel;
                    try{
                        List<Message> messages = channel1.getHistory().retrievePast(50).complete();
                        for(Message msg : messages){
                            if(msg.getAuthor().getEffectiveName().equals("UntisBot")){
                                msg.delete().queue();
                            }
                        }
                    }catch (Exception e){}
                }
            }
        }

        Thread t = new Thread(() -> {
            while (true) {
                    try{
                        for(ServerData data : serverData){
                            try{
                                refreshHours(data);
                                refreshEmbeds(data.messageID);
                            }catch (Exception ignored){

                            }
                        }
                        List<String> times = List.of("6:0","7:0");
                        if(times.contains(getTime().getHour() + ":" + getTime().getMinute())){
                            for(Message msg : messagesPings){
                                try{
                                    if(msg.getAuthor().getEffectiveName().equals("UntisBot")){
                                        msg.delete().queue();
                                    }
                                }catch (Exception ignored){}
                            }
                            messagesPings.clear();
                            if(stundenEntfallen){
                                /**
                                 * TODO read PINGS
                                 */
                                //messagesPings.add(chatChannel.sendMessage("<@&" + pingRoleID + "> Es Entfallen Stunden!").complete());
                                stundenEntfallen = false;
                            }
                        }
                    } catch (Exception ignored){}
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        reactionReady = true;

        for(Guild g : jda.getGuilds()){
            g.updateCommands().addCommands(Commands.slash("setup", "Setup UntisBot").addOptions(
                            new OptionData(OptionType.STRING, "setoffset", "Time Offset"),
                            new OptionData(OptionType.STRING, "schoolname", "Schulname"),
                            new OptionData(OptionType.STRING, "password", "Passwort"),
                            new OptionData(OptionType.STRING, "classname", "Klassenname")),
                    Commands.slash("convert", "Convert number systems").addOptions(
                            new OptionData(OptionType.INTEGER, "from", "Original System"),new OptionData(OptionType.INTEGER,"to","to System"),new OptionData(OptionType.STRING,"data","The number")
                    )).queue();
        }
        System.out.println("Commands loaded");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("setup")){
            ServerData data = new ServerData();
            System.out.println(event.getOptions().size());
            if(event.getOptions().size() == 1){
                hourOffset = event.getOptions().get(0).getAsInt();
                System.out.println(hourOffset);
                event.reply("Changed time offset to " + hourOffset).setEphemeral(true).queue();
            }else if(event.getOptions().size() == 3) {
                try{
                    TextChannel chatChannel = event.getChannel().asTextChannel();
                    List<Message> messages = chatChannel.getHistory().retrievePast(50).complete();
                    for(Message msg : messages){
                        if(msg.getAuthor().getEffectiveName().equals("UntisBot")){
                            msg.delete().queue();
                        }
                    }
                    String reactionID = sendMessage("React for Ping role",chatChannel.getId());
                    String timetableID = sendMessage("Timetable Message",chatChannel.getId());
                    data.className = event.getOption("classname").getAsString();
                    data.password = event.getOption("password").getAsString();
                    data.schoolName = event.getOption("schoolname").getAsString();
                    data.reactionMessageID = reactionID;
                    data.messageID = timetableID;
                    data.messageChannel = event.getChannel().getId();
                    refreshHours(data);
                    refreshEmbeds(timetableID);
                    serverData.add(data);
                }catch (Exception e){
                    event.reply("Setup error!").setEphemeral(true).queue();
                }
                event.reply("Setup done").setEphemeral(true).queue();
            } else {
                event.reply("Setup error!").setEphemeral(true).queue();
            }
        } else if(event.getName().equals("convert")){
            for(OptionMapping o : event.getOptions()){
                System.out.println(o.getAsString());
            }
            if(event.getOptions().size() != 3){
                event.reply("Invalid command usage!").setEphemeral(true).queue();
            } else {
                int from = event.getOptions().get(0).getAsInt();
                int to = event.getOptions().get(1).getAsInt();
                String value = event.getOptions().get(2).getAsString();

                if(from >= 36 || from <= 1){
                    event.reply("Das System muss mindestens 2 und maximal 36 sein! Angegeben: " + from).setEphemeral(true).queue();
                } else if(to >= 36 || to <= 1){
                    event.reply("Das System muss mindestens 2 und maximal 36 sein! Angegeben: " + to).setEphemeral(true).queue();
                } else {
                    String returnValue = toSystem(to,fromSystem(from,value));
                    event.reply("Result: " + returnValue + " (Decimal: " + fromSystem(from,value) + ")").setEphemeral(true).queue();
                }
            }
        }
    }

    public static String sendMessage(String content,String channelID){
        content = content.replaceAll("_","\\_");
        TextChannel chatChannel = jda.getTextChannelById(channelID);
        if(chatChannel == null){
            return null;
        }
        Message msg = chatChannel.sendMessage(content).complete();
        return msg.getId();
    }

    /*public static void generateImage(String id) throws IOException {
        try{
            BufferedImage image = new BufferedImage(1000,2000,1);

            Graphics2D g = image.createGraphics();
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0,0,200,2000);
            g.fillRect(0,0,1000,50);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);

            chatChannel.editMessageAttachmentsById(id,AttachedFile.fromData(bytes.toByteArray(),"image.png")).setContent("# Aktueller Stundenplan\nAktualisiert: " + "<t:" + (System.currentTimeMillis()/1000) + ":R>\nDatum: " + getDate() + ", " + LocalTime.now(ZoneOffset.systemDefault())).queue();
        }catch (Exception e){
            e.printStackTrace();
        }
    }*/

    public static void refreshEmbeds(String id){
        for(ServerData data : serverData){
            List<MessageEmbed> embeds = new ArrayList<>();
            EmbedBuilder eb = new EmbedBuilder();
            if(stundeHashMap.size() != 0){
                for(int i = 0; i <10;i++){
                    eb.clearFields();
                    if(stundeHashMap.containsKey(i+1)){
                        Stunde s = stundeHashMap.get(i+1);
                        eb.setColor(s.getColor());
                        if(s.getInfo().equals("REGULAR")){
                            eb.addField(s.getStunde() + ". Stunde " + s.getName(), s.getTimes() + " in Raum: " + s.getRoom(), true);
                        } else {
                            eb.addField(s.getStunde() + ". Stunde " + s.getName() + " (" + s.getInfo() + ")", s.getTimes() + " in Raum: " + s.getRoom(), true);
                        }
                        embeds.add(eb.build());
                    }
                }
            } else {
                eb.setColor(Color.RED);
                eb.addField("Kein Stundenplan", "Aktuell kein Stundenplan verfügbar!", true);
                embeds.add(eb.build());
            }
            try{
                System.out.println(jda);
                jda.getTextChannelById(data.messageChannel).editMessageById(data.messageID,"# Aktueller Stundenplan für " + data.className + " \nAktualisiert: " + "<t:" + (System.currentTimeMillis()/1000) + ":R>\nDatum: " + refreshTime.getDayOfWeek() + " " + refreshTime.getDayOfMonth() + "." + refreshTime.getMonth().getValue() + "." + refreshTime.getYear()).setEmbeds(embeds).queue();
            }catch (Exception e){
               System.err.println("Error updating message");
               e.printStackTrace();
            }
            System.out.println("Updated " + data.toString());
        }

    }

    public static void refreshHours(ServerData data){
            try {
                session = Session.login(data.className,data.password, "https://mese.webuntis.com", data.schoolName);
                Classes classes = session.getClasses();
                int id = 0;
                for (Classes.ClassObject classObject : classes.searchByName(data.className)) {
                    id = classObject.getId();
                }
                session.useCache(false);
                Timetable timetable;
                boolean refreshLetzeStunde;
                if(localTimeToLong(letzteStunde)<=localTimeToLong(getTime())){
                    timetable = session.getTimetableFromClassId(getDate().plusDays(1), getDate().plusDays(1), id);
                    refreshLetzeStunde = false;
                    refreshTime = getDate().plusDays(1);
                } else {
                    timetable = session.getTimetableFromClassId(getDate(), getDate(), id);
                    refreshLetzeStunde = true;
                    refreshTime = getDate();
                }
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

                        if(refreshLetzeStunde){
                            letzteStunde = timetable.getEndTimes().get(i);
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
                        long now = localTimeToLong(getTime());
                        if(s.getInfo().equals("IRREGULAR")){
                            s.setColor(Color.ORANGE);
                            stundenEntfallen = true;
                            if(now <= start && now >= (start-300)){
                                s.setColor(Color.ORANGE);
                            }else if(now <= end && now > start){
                                s.setColor(Color.YELLOW);
                            }
                        }
                        if(s.getInfo().equals("CANCELLED")){
                            s.setColor(Color.RED);
                            stundenEntfallen = true;
                            if(now <= start && now >= (start-300)){
                                s.setColor(Color.MAGENTA);
                            }else if(now <= end && now > start){
                                s.setColor(Color.MAGENTA);
                            }
                        }
                        if(s.getInfo().equals("REGULAR")){
                            s.setColor(Color.GREEN);
                            if(now <= start && now >= (start-300)){
                                s.setColor(Color.CYAN);
                            }else if(now <= end && now > start){
                                s.setColor(Color.BLUE);
                            }
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
        System.out.println(event.getUser().getName() + " reacted " + event.getReaction());
        if(!reactionReady || Objects.requireNonNull(event.getUser()).getName().equals("UntisBot")){
            return;
        }
        for(ServerData data : serverData){
            if(!event.getChannel().getId().equals(data.messageChannel)){
                return;
            }
        }

        TextChannel chatChannel = jda.getTextChannelById(event.getChannel().getId());

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
                } else {
                    event.getGuild().addRoleToMember(event.getUser(),role).queue();
                }
            } else {
                role = chatChannel.getGuild().getRoleById(pingRoleID);
                if(role != null){
                    if(Objects.requireNonNull(event.getMember()).getRoles().contains(role)){
                        event.getGuild().removeRoleFromMember(event.getUser(),role).queue();
                    } else {
                        event.getGuild().addRoleToMember(event.getUser(),role).queue();
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
    public static long fromSystem(int size, String value){
        char[] chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        int i = value.length()-1;
        long output = 0;
        for(char c : value.toCharArray()){
            int val = 0;
            boolean found = false;
            for(char c0 : chars){
                if(c0 == c) {
                    found = true;
                    break;
                }
                val++;
            }
            if(!found) return -1;
            output+= Math.pow(size,i)*val;
            i--;
        }
        return output;
    }

    public static String toSystem(int system, long value){
        char[] chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        int length = 0;
        while(Math.pow(system,length)<=value) length++;
        String output = "";
        while(length>=0){
            char c = '-';
            int j = 0;
            for(int i = 0; i<system;i++){
                int e = (int) Math.pow(system,length)*i;
                if(e<=value){
                    c=chars[i];
                    j = e;
                }
            }
            output = output + c;
            value = value - j;
            length--;
        }
        return output.substring(1);
    }
}