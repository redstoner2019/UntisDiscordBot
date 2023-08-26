package me.redstoner2019;

import java.awt.*;

public class Stunde {
    public String name;
    public int stunde;
    public Color color;
    public String times;
    public String info;
    public String teacher;
    public String room;
    public Stunde(String name, Color color, String times,String info, String teacher, String room){
        this.name = name;
        this.color = color;
        this.times = times;
        this.info = info;
        this.teacher = teacher;
        this.room = room;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public int getStunde() {
        return stunde;
    }

    public void setStunde(int stunde) {
        this.stunde = stunde;
    }

    public String getTimes() {
        return times;
    }

    public void setTimes(String times) {
        this.times = times;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }
}
