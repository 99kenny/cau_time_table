package org.example;

import org.example.Secrets.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.List;

public class Crawl {
    public static final String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static final String WEB_DRIVER_PATH = "src/main/resources/chromedriver.exe";
    public static final int SCROLL_TIME_OUT = 10;

    public static final String ID = LoginInfo.ID;

    public static final String PWD = LoginInfo.PWD;

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

        String url = "https://cau.everytime.kr/timetable";

        //요청할 URL을 get()에 전달하면 응답된 페이지를 브라우저를 통해 확인할 수 있다.
        cd.get(url);

        //로그인
        List<WebElement> form = cd.findElementsByTagName("input");
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
        while (true) { //30초 동안 무한스크롤 지속
            //<tr> 개수
            num = cd.findElementsByTagName("tr").size();
            try {
                Thread.sleep(500); //리소스 초과 방지
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //executeScript: 해당 페이지에 JavaScript 명령을 보내는 거
            ((JavascriptExecutor)cd).executeScript("arguments[0].scrollBy(0, document.querySelector(\"div.list\").scrollHeight)", scroll);
            //<tr> 개수 그대로면 아직 ajax 로딩 안된거니까 그때 까지 기다린다. 근데 10초이상 걸리면 끝까지 내려간거니까 종료
            try {
                new WebDriverWait(cd, SCROLL_TIME_OUT).until(
                        ExpectedConditions.numberOfElementsToBeMoreThan(By.tagName("tr"), num));
            } catch (TimeoutException e){
                break;
            }
        }
        //테이블 태그에 있는거 다 가져오기
        List<WebElement> courseInfo = cd.findElementByCssSelector("div.list").findElement(By.tagName("table")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
        for (WebElement course : courseInfo) {
            List<WebElement> infos = course.findElements(By.tagName("td"));
            String code = infos.get(2).getText();
            String name = infos.get(3).getText();
            String prof = infos.get(4).getText();
            String time = infos.get(7).getText();
            String note = infos.get(11).getText();
        }

        cd.close();
        cd.quit();
    }
}
