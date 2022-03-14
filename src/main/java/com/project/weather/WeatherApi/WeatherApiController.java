package com.project.weather.WeatherApi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneId;
import java.util.HashMap;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
    @RestController : 기본으로 하위에 있는 메소드들은 모두 @ResponseBody를 가지게 된다.
    @RequestBody : 클라이언트가 요청한 XML/JSON을 자바 객체로 변환해서 전달 받을 수 있다.
    @ResponseBody : 자바 객체를 XML/JSON으로 변환해서 응답 객체의 Body에 실어 전송할 수 있다.
            클라이언트에게 JSON 객체를 받아야 할 경우는 @RequestBody, 자바 객체를 클라이언트에게 JSON으로 전달해야할 경우에는 @ResponseBody 어노테이션을 붙여주면 된다.
    @ResponseBody를 사용한 경우 View가 아닌 자바 객체를 리턴해주면 된다.
*/
@RestController
@RequestMapping("/api")
public class WeatherApiController {

    private String ApiType;
    private int pageRows,pageNo,date,time,nx,ny;
    private String[] informations = new String[6];
    private final String ServiceKey  = "?serviceKey=GTWxvGjFFaMggZ6YGgtXbinNgewRheMqi9JVYQMUw26gJSpug6HjPTKyf0JzqJWkNXBT1%2Bn0dHmeS3rEDVum1A%3D%3D";

    @GetMapping("/weather/{apitype}/{info}")
    public String restApiGetWeather(@PathVariable("apitype") String apitype,@PathVariable("info") String info) throws Exception {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formatedNow = now.format(formatter);

        //XSSFWorkbook workbook = new XSSFWorkbook()

        this.ApiType = getApiType(apitype);

        informations = info.split("&");
        for(int i=0; i< informations.length; i++)
            informations[i] = informations[i].replaceAll("[^0-9]","");

        StringBuffer url = new StringBuffer("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/");
        url.append(this.ApiType); //api type
        url.append(ServiceKey); //인증키
        url.append("&dataType=JSON"); // JSON, XML
        url.append("&numOfRows="+informations[0]);             // 페이지 ROWS
        url.append("&pageNo="+informations[1]);                 // 페이지 번호
        url.append("&base_date=" + formatedNow); // 발표일자
        url.append("&base_time="+informations[2]);           // 발표시각
        url.append("&nx="+informations[3]);                    // 예보지점 X 좌표
        url.append("&ny="+informations[4]);                  // 예보지점 Y 좌표

        System.out.println(url.toString());
        HashMap<String, Object> resultMap = getDataFromJson(url.toString(), "UTF-8", "get", "");

        JSONObject jsonObj = new JSONObject();

        jsonObj.put("result", resultMap);

        return jsonObj.toString();
    }

    public HashMap<String, Object> getDataFromJson(String url, String encoding, String type, String jsonStr) throws Exception {
        boolean isPost = false;

        if ("post".equals(type)) {
            isPost = true;
        } else {
            url = "".equals(jsonStr) ? url : url + "?request=" + jsonStr;
        }

        return getStringFromURL(url, encoding, isPost, jsonStr, "application/json");
    }

    public HashMap<String, Object> getStringFromURL(String url, String encoding, boolean isPost, String parameter, String contentType) throws Exception {
        URL apiURL = new URL(url);

        HttpURLConnection conn = null;
        BufferedReader br = null;
        BufferedWriter bw = null;

        HashMap<String, Object> resultMap = new HashMap<String, Object>();

        try {
            conn = (HttpURLConnection) apiURL.openConnection();
            conn.setConnectTimeout(100000);
            conn.setReadTimeout(100000);
            conn.setDoOutput(true);

            if (isPost) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", contentType);
                conn.setRequestProperty("Accept", "*/*");
            } else {
                conn.setRequestMethod("GET");
            }

            conn.connect();

            if (isPost) {
                bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                bw.write(parameter);
                bw.flush();
                bw = null;
            }

            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));

            String line = null;

            StringBuffer result = new StringBuffer();

            while ((line=br.readLine()) != null) {result.append(line);}

            ObjectMapper mapper = new ObjectMapper();

           // System.out.println("json 출력" + result.toString());

            resultMap = mapper.readValue(result.toString(), HashMap.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(url + " interface failed" + e.toString());
        } finally {
            if (conn != null) conn.disconnect();
            if (br != null) br.close();
            if (bw != null) bw.close();
        }

        return resultMap;
    }

    public String getApiType(String ApiType)
    {
        /*
            @ API LIST ~
            getUltraSrtNcst 초단기실황조회
            getUltraSrtFcst 초단기예보조회
            getVilageFcst 동네예보조회
            getFcstVersion 예보버전조회
        */
        String str = "";
        switch (ApiType)
        {
            case "ultra-srtncst": str = "getUltraSrtNcst";
            break;
            case "ultra-srtfcst": str = "getUltraSrtFcst";
            break;
            case "vilage-fcst": str = "getVilageFcst";
            break;
            case "fcst-version": str = "getFcstVersion";
            break;
        }
        return str;
    }
}