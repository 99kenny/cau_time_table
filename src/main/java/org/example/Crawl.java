package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.example.Secrets.WEB_DRIVER_ID;
import static org.example.Secrets.WEB_DRIVER_PATH;
import static org.example.Secrets.ID;
import static org.example.Secrets.PWD;

public class Crawl {
    public static final int SCROLL_TIME_OUT = 10;
    //2022년 2학기 - 2628개
    public static final int MIN_TAG_NUM = 2500;

    private Repository repository;
    private List<Course> newCourses;
    private List<Time> newTimes;

    Crawl(){
        newCourses = new ArrayList<Course>();
        newTimes = new ArrayList<Time>();
    }
    public void get(){
        //해당 브라우저에 다양한 옵션을 주기위해 ChromeOptions 객체화
        ChromeOptions options = new ChromeOptions();
        //옵션 설정
        //headless : 브라우저 실행이 내부로 전환된다, 눈에 안보인다.
        //options.addArguments("headless");


        //운영체제에 드라이버 설정
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);

        //설정한 옵션 객체를 ChromeDriver 생성자를 통해 전달한다.
        ChromeDriver cd = new ChromeDriver(options);

        options.setBinary("/path/to/other/chrome/binary");
        cd.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);

        String url = "https://cau.everytime.kr/timetable";

        //요청할 URL을 get()에 전달하면 응답된 페이지를 브라우저를 통해 확인할 수 있다.
        System.out.println("url loading...");
        cd.get(url);
        //로그인
        List<WebElement> form = cd.findElementsByTagName("input");
        //cd.executeScript("document.getElementsByTagName('input')[0].value="+ID);
        form.get(0).sendKeys(ID);
        form.get(1).sendKeys(PWD);
        cd.findElementByClassName("submit").click();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //수업 목록에서 검색 클릭
        cd.findElementByCssSelector("li.button.search").click();

        WebElement scroll = cd.findElementByCssSelector("div.list");
        int num = 0;
        int cnt = 0;
        System.out.println("Scroll Down...");
        while (true) { //30초 동안 무한스크롤 지속
            //<tr> 개수 //이게 수업 수는 아님 //
            num = cd.findElementsByTagName("tr").size();

            //executeScript: 해당 페이지에 JavaScript 명령을 보내는 거
            ((JavascriptExecutor)cd).executeScript("arguments[0].scrollBy(0, document.querySelector(\"div.list\").scrollHeight)", scroll);
            //<tr> 개수 그대로면 아직 ajax 로딩 안된거니까 그때 까지 기다린다. 근데 10초이상 걸리면 끝까지 내려간거니까 종료
            System.out.println("Scrolling... " + String.valueOf((((float)num) / MIN_TAG_NUM) * 100) + "%");
            try {
                new WebDriverWait(cd, SCROLL_TIME_OUT).until(
                        ExpectedConditions.numberOfElementsToBeMoreThan(By.tagName("tr"), num));
            } catch (TimeoutException e){
                if(num > MIN_TAG_NUM){
                    break;
                }
            }
        }
        //테이블 태그에 있는거 다 가져오기
        List<WebElement> courseInfo = cd.findElementByCssSelector("div.list").findElement(By.tagName("table")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));

        for (WebElement course : courseInfo) {
            List<WebElement> infos = course.findElements(By.tagName("td"));
            CrawlData crawlData = CrawlData.builder()
                    .code(infos.get(2).getText())
                    .name(infos.get(3).getText())
                    .prof(infos.get(4).getText())
                    .timeInfo(infos.get(7).getText())
                    .note(infos.get(11).getText()).build();
            System.out.println("found : " + crawlData.toString());
            if (!parseString(crawlData)) {
                return;
            }
        }
        cd.close();
        cd.quit();

        repository = new Repository();
        newCourses.forEach(newCourse -> repository.insertCourse(newCourse));
        newTimes.forEach(newTime -> repository.insertTime(newTime));
        repository.commit();
        repository.close();

    }

    public Boolean parseString(CrawlData crawlData) {
        String timeInfo = crawlData.getTimeInfo();
        String[] splitted = timeInfo.split("/");
        String building = "";
        String room = "";
        String week = "";
        String time = "";
        String temp = "";
        for (int i = 0; i < splitted.length; i++) {
            if(splitted[i].length() <= 1){
                continue;
            }
            splitted[i] = splitted[i].trim();
            //시간
            if((splitted[i].charAt(0) == '월' ||
                    splitted[i].charAt(0) == '화' ||
                    splitted[i].charAt(0) == '수' ||
                    splitted[i].charAt(0) == '목' ||
                    splitted[i].charAt(0) == '금' ||
                    splitted[i].charAt(0) == '토' ||
                    splitted[i].charAt(0) == '일'))
            {

                splitted[i] = splitted[i].trim();
                //00 0000
                if(splitted[i].charAt(0) == '0'){
                    continue;
                }
                week = String.valueOf(splitted[i].charAt(0));
                time = "";
                //시간 형태
                //case1 월15:00~16:15, 수15:00~16:15
                //case2 월(13:30~14:45) / 수(13:30~14:45)
                //case3 월15:00~16:15 / 수15:00~16:15
                splitted[i] = splitted[i].replaceAll("[(]", "");
                splitted[i] = splitted[i].replaceAll("[)]","");
                //case2 -> case3
                if(splitted[i].contains(":")) {
                    if (splitted[i].contains(",")) {
                        String[] splitTime = splitted[i].split(",");
                        for (int j = 0; j < splitTime.length; j++) {
                            week = String.valueOf(splitTime[j].trim().charAt(0));
                            time = splitTime[j].trim().substring(1);

                            newTimes.add(Time.builder()
                                    .week(week)
                                    .time(time)
                                    .courseId(crawlData.getCode()).build());
                            System.out.println("--" + newTimes.get(newTimes.size() - 1));
                        }
                    }
                    else {
                        time = splitted[i].trim().substring(1);
                        newTimes.add(Time.builder()
                                .week(week)
                                .time(time)
                                .courseId(crawlData.getCode()).build());
                        System.out.println("--" + newTimes.get(newTimes.size() - 1));
                    }
                }
                //교시 형태
                else {
                    String start = "";
                    String end = "";
                    time = splitted[i].substring(1);
                    if (time.length() == 1) {
                        start = toTime(time.substring(0, 1));
                        time = start + "~" + afterOneHour(start);
                    } else {
                        String[] classt = time.split(",");
                        start = toTime(classt[0].trim());
                        end = toTime(classt[classt.length - 1].trim());
                        time = start + "~" + afterOneHour(end);
                    }

                    newTimes.add(Time.builder()
                            .week(week)
                            .time(time)
                            .courseId(crawlData.getCode()).build());
                    System.out.println("--"+newTimes.get(newTimes.size()-1));
                }
            }
            //강의실
            else {
                splitted[i] = splitted[i].trim();
                if (splitted[i].charAt(0) == '0') {

                } else if(splitted[i].contains("관")){
                    String[] loc = splitted[i].split(" ");
                    //102관(약학대학 및 R&D센터) 501호
                    //208관(제2공) 208호
                    //310관 310호
                    //810관 209호 / 311호
                    if(loc.length > 1) {
                        building = loc[0].substring(0, 4);

                        room = loc[loc.length - 1];
                        temp = building;
                    }
                }
                else {
                    if(temp == ""){
                        System.out.println("WARNING : might be room without building");
                    }
                    //hot fix
                    if(temp.equals(newCourses.get(newCourses.size()-1).getBuilding())){
                        building = temp;
                        room = splitted[i].trim();
                        Course lastUpdate = newCourses.get(newCourses.size()-1);
                        newCourses.remove(newCourses.size()-1);
                        lastUpdate.setRoom(lastUpdate.getRoom() + "," + room);
                        newCourses.add(lastUpdate);
                        System.out.println("-- fix : "+newCourses.get(newCourses.size()-1));
                    }

                }
                newCourses.add(Course.builder(crawlData.getCode())
                        .name(crawlData.getName())
                        .prof(crawlData.getProf())
                        .note(crawlData.getNote())
                        .building(building)
                        .room(room).build());
                System.out.println("--"+newCourses.get(newCourses.size()-1));
            }
        }

        return true;
    }

    public String afterOneHour(String time){
        String start = time.substring(0,2);
        return String.valueOf(Integer.parseInt(start) + 1) + ":00";
    }
    public String toTime(String classTime){
        switch (classTime){
            case "0":
                return "08:00";
            case "1":
                return "09:00";
            case "2":
                return "10:00";
            case "3":
                return "11:00";
            case "4":
                return "12:00";
            case "5":
                return "13:00";
            case "6":
                return "14:00";
            case "7":
                return "15:00";
            case "8":
                return "16:00";
            case "9":
                return "17:00";
            case "10":
                return "18:00";
            case "11":
                return "19:00";
            case "12":
                return "20:00";
            case "13":
                return "21:00";
            case "14":
                return "22:00";
            default:
                return "";
        }
    }
}
