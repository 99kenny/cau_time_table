package org.example;

import java.sql.*;

import static org.example.Secrets.*;

public class Repository {
    Connection conn;

    public Repository(){
        try {
            conn = null;
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DBURL, DBUSER, DBPWD);
            conn.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean insertCourse(Course course){
        String id = course.getId().toString();
        String code = course.getCode();
        String name = course.getName();
        String prof = course.getProf();
        String note = course.getNote();
        String building = course.getBuilding();
        String room = course.getRoom();
        System.out.println("insert: " + id + " " + name);
        String checkSql = "SELECT * FROM course WHERE id = " + course.getId();
        if(execute(checkSql) == 1){
            System.out.println("exist: " + course.getId() + " : " + course.getCode());
        }
        else{
            String sql = "INSERT INTO course(id,code, name,prof,note,building,room) VALUES("
                    + id + ","
                    + code + ","
                    + name + ","
                    + prof + ","
                    + note + ","
                    + building + ","
                    + room + ")";
            int result = execute(sql);
            if(result == 1){
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean insertTime(Time time){
        String id = String.valueOf(time.getId());
        String week = time.getWeek();
        String time_p = time.getTime();
        String courseId = time.getCourseId();

        System.out.println("insert: " + courseId);
        String sql = "INSERT INTO time(id,week,time,course_id) VALUES("
                + id + ","
                + week + ","
                + time_p + ","
                + courseId + ")";
        int result = execute(sql);
        if(result == 1){
            return true;
        } else {
            return false;
        }
    }

    public int execute(String sql){
        PreparedStatement ps = null;
        int rs = 0;
        try {
            ps = conn.prepareStatement(sql);
            rs = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return rs;
    }

    public void close(){
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void commit(){
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
