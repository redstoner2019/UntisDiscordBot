package me.redstoner2019;

public class ServerData {
    public String schoolName = "";
    public String password = "";
    public String className = "";
    public String messageID = "";
    public String reactionMessageID = "";
    public String messageChannel = "";
    public ServerData(){

    }

    @Override
    public String toString() {
        return "ServerData{" +
                "schoolName='" + schoolName + '\'' +
                ", password='" + password + '\'' +
                ", className='" + className + '\'' +
                ", messageID='" + messageID + '\'' +
                ", reactionMessageID='" + reactionMessageID + '\'' +
                '}';
    }
}
