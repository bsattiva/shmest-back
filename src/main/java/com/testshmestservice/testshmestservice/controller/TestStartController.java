package com.testshmestservice.testshmestservice.controller;

import com.enums.Area;
import com.utils.FileHelper;
import com.utils.Helper;
import com.utils.HtmlHelper;
import com.utils.RequestHelper;
import com.utils.TestHelper;
import com.utils.command.CommandRunner;
import com.utils.data.QueryHelper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.report.Step;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


@RestController
@EnableAutoConfiguration
@SpringBootApplication
@CrossOrigin
public class TestStartController {

    @Getter
    private static final String TEST_PROJECT = "TestCube";
    @Getter
    private static final String PROJECT = "project";

    private static final String TOKEN = "token";
    private static final String RUN_ID = "runId";
    private static final String STEP_ID = "stepId";

    private static final String MASK = "?";
    private static final String QUESTION_MASK = "\\?";
    private static final String DEFAULT_FEATURE = "/src/test/features/defaultFeature.feature";
    private static final String PAGES = "/src/test/resources/pages.json";
    private static final String CONFIG = "/src/test/resources/config.properties";
    private static final String PAGE_NAME = "pageName";
    private static final String HTML = "html";
    private static final Logger LOGGER = Logger.getLogger(TestStartController.class);
    private static final String CUBE_SECRET = "cubeSecret";
    private static final String SECRET = (Helper.isThing(System.getProperty(CUBE_SECRET)))
            ? System.getProperty(CUBE_SECRET) : "92jehdjasdg823jewgahuawaw wsau7a";


    @PostMapping("/record-step")
    JSONObject recordTestStep(final HttpServletRequest request, final HttpServletResponse response) {
        var object = RequestHelper.getRequestBody(request);
        var step = new Step();
        step.setName(object.getString("name"));
        step.start();
        step.complete(true);
        step.getHtml();
        return new JSONObject();
    }


    @GetMapping("/single-page")
    String getPage(final HttpServletRequest request, final HttpServletResponse response) {

        var project = getProject(request);
        var name = request.getParameter("id");

        return QueryHelper.getSinglePage(project,name).toString();

    }


    @GetMapping("/pages")
    String getPages(final HttpServletRequest request, final HttpServletResponse response) {
        var token = request.getParameter(TOKEN).replace(" ", "+");
        var project = getProject(request);
        var query = "select pagename,page,url from shmest.pages where projectid='?'".replace(MASK, project);
        var data = QueryHelper.getData(query, "pull-table");
        var escapedData = data.getJSONArray("message").toString()
                .replace(":\"{", ":{")
                .replace("}\",", "},")
                .replace("[\"{","[{")
                .replace("}\"]", "}]")
                .replace("\\", "")
                .replaceAll("\"\"([a-zA-Z0-9]*)\"\"", "\"\"$1\"\"")
                .replace("u00027", "'")
                .replace("\"[{", "[{")
                .replace("}]\"", "}]")
;
        System.out.println(escapedData);
        var array =  new JSONArray(escapedData);
        for (var i = 0; i < array.length(); i++) {
            var object = array.getJSONObject(i);
            var arr = new JSONArray();
            if (object.get("page") instanceof JSONObject) {
                var keys = object.getJSONObject("page").keys();
                while (keys.hasNext()) {
                    var key = keys.next();
                    var obj = object.getJSONObject("page").getJSONObject(key);
                    arr.put(obj);
                }

                object.remove("page");
                System.out.print(object.toString(5));
                object.put("page", arr);

            }


        }

        return array.toString();
    }


    @GetMapping("/auth")
    String getProject(final HttpServletRequest request){
        String token = request.getParameter(TOKEN).replace(" ", "+");
        return QueryHelper.getProject(token);
    }

    @GetMapping("/parse")
    String getLocalHtml(final HttpServletRequest request) {
        var url = request.getParameter("url");
        var pageName = request.getParameter("name");
        String content = null;
        URLConnection connection = null;
        try {
            connection =  new URL(url).openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            scanner.close();
        }catch ( Exception ex ) {
            ex.printStackTrace();
        }
        var html = HtmlHelper.parseHtml(content);
        var htmlObject = new JSONObject();
        htmlObject.put(PAGE_NAME, pageName);
        htmlObject.put("elements", html);
        return htmlObject.toString();
    }

    @PostMapping("/signin")
    String signIn(final HttpServletRequest request, final HttpServletResponse response) {
        var obj = RequestHelper.getRequestBody(request);
        var body = new JSONObject();
        body.put("email", obj.getString("email"));
        body.put("password", obj.getString("password"));
        return QueryHelper.postData(Helper.getUrl("auth.url") + "/login", body).toString();
    }
    @GetMapping("/single-test")
    String getSingleTest(final HttpServletRequest request, final HttpServletResponse response) {
        var token = request.getParameter("token");
        var project = QueryHelper.getProject(token);
        var status = Helper.getFailedObject();
        if (Helper.isThing(project)) {
            var id = request.getParameter("id");
            status = QueryHelper
                    .getData("select name,test from shmest.tests where id='" + id + "'", "pull-table");

        } else {
            response.setStatus(401);
        }
        return status.toString();
    }

    @PostMapping("/save-test")
    String saveTest(final HttpServletRequest request, final HttpServletResponse response) {
        var object = RequestHelper.getRequestBody(request);
        var token = object.getString("token");
        var status = Helper.getFailedObject();
        var project = QueryHelper.getProject(token);
        if (Helper.isThing(project)) {
            var tests = object.getString("test");
            var id = object.getString("id");
            var name = object.getString("name");
            var query = "insert into shmest.tests values('?','?','?','?','')"
                    .replaceFirst(QUESTION_MASK, id)
                    .replaceFirst(QUESTION_MASK, project)
                    .replaceFirst(QUESTION_MASK, name)
                    .replaceFirst(QUESTION_MASK, tests);
            var idQuery = "select id from shmest.tests where id='" + id + "' and project='" + project + "'";
            var savedId = QueryHelper.getData(idQuery, "pull-string");
            if (savedId != null
                    && savedId.has("message")
                    && Helper.isThing(savedId.getString("message"))) {
                query = "update shmest.tests set test='"
                        + tests + "' where id='" + id + "' and project='" + project + "'";
            }
            var stat = QueryHelper.getData(query, "execute");
            status.put("message", stat.getString("message"));
        } else {
            response.setStatus(401);

        }
        return status.toString();
    }

    @GetMapping("/tests")
    String getTests(final HttpServletRequest request, final HttpServletResponse response) {
        String token = request.getParameter(TOKEN).replace(" ", "+");
        var project = QueryHelper.getProject(token);
        var query = "select id,name from shmest.tests where project='?'".replace(MASK, project);
        var data = QueryHelper.getData(query, "pull-table");

        return data.getJSONArray("message").toString();
    }

    @GetMapping("/defs")
    String getDefs() throws IOException {
        var defs = FileHelper.getResourceFile("defs.json", false);
        return FileUtils.readFileToString(defs, StandardCharsets.UTF_8);

    }

    @PostMapping("/post-page")
    @ResponseBody
    String postPage(final HttpServletRequest request, final HttpServletResponse response) {
        var result = new JSONObject();
        var object = RequestHelper.getRequestBody(request);
        var token = object.getString(TOKEN);
        var project = QueryHelper.getProject(token);
        if (Helper.isThing(project)) {
            var page = object.getJSONObject("pageobject");
            page.put("project", project);
            var persisted = QueryHelper.persistSavedPage(page);
            if (persisted.getString("message").equals("success")) {
                result = QueryHelper.getSinglePage(project, page.getString("pagename"));
            }
        }

        return result.toString();
    }

    @PostMapping("/html")
    @ResponseBody
    String readHtml(final HttpServletRequest request, final HttpServletResponse response) {
        var result = new JSONObject();
        var object = RequestHelper.getRequestBody(request);
        var pageName = object.getString(PAGE_NAME);
        var html = object.getString(HTML);
        if (Helper.isThing(html)) {
            var pageObject = HtmlHelper.parseHtml(html);
            result.put(PAGE_NAME, pageName);
            result.put("elements", pageObject);
            result.put("project", object.getString("project"));
            result.put("url", object.getString("url"));
            System.out.print(result.toString(5));
            System.out.print(QueryHelper.persistPage(result));

        }

        return result.toString();
    }

    @PostMapping("/step")
    String saveStep(final HttpServletRequest request, final HttpServletResponse response) {
        var object = RequestHelper.getRequestBody(request);
        var secret = object.getString("secret");
        var result = new JSONObject("{\"message\":{\"status\":\"failure\"}}");
        System.out.println("SAVING STEP");
        if (secret.equals(SECRET)) {
            var statObject = new JSONObject();
            statObject.put("status",  QueryHelper.saveRunStep(object));
            result.put("message", statObject);
        } else {
            response.setStatus(401);
        }

        return result.toString();
    }

    @GetMapping("/log")
    String getLogs(final HttpServletRequest request, final HttpServletResponse response) {
        var project = getProject(request);
        var records = (Helper.isThing(request.getParameter("records")))
                ? Integer.parseInt(request.getParameter("records")) : 0;
        var result = new JSONObject();
        if (Helper.isThing(project)) {
            if (Helper.isThing(request.getParameter("area"))) {
                result = QueryHelper.getLogs(records, Area.valueOf(request.getParameter("area").toUpperCase()));
            } else {

                result = QueryHelper.getLogs(records);
            }

        } else {
            QueryHelper.logEntry("UNAUTHORIZED ACCESS TO LOGS ATTEMPTED", "", Area.QUERY_HELPER.label);
            response.setStatus(401);
        }
        return result.toString();
    }

    @GetMapping("/step-stat")
    String getStepStat(final HttpServletRequest request, final HttpServletResponse response) {
        var project = getProject(request);
        var result = new JSONObject();
        result.put("message", TestHelper.getStatus("pending"));
        if (Helper.isThing(project)) {
            return QueryHelper.getStepStatus(request.getParameter(RUN_ID), request.getParameter(STEP_ID), project);
        } else {
            response.setStatus(401);
        }
        return result.toString();
    }

    static class Runner extends Thread {
        @SneakyThrows
        public void run() {
            CommandRunner.runCommand();
        }

    }

    @PostMapping("/start")
    String startTest(final HttpServletRequest request, final HttpServletResponse response) throws java.net.MalformedURLException {
        var result = new JSONObject();

        var runId = Helper.getRandomString(8);
        var object = RequestHelper.getRequestBody(request);
        if(object.has(RUN_ID) && !object.getString(RUN_ID).equals("undefined")) {
            runId = object.getString(RUN_ID);
        }

        var scenarioId = (object.has("scenarioId"))
                ? object.getString("scenarioId") : Helper.getRandomString(10);
        var token = object.getString(TOKEN);
        var project = QueryHelper.getProject(token);
        var url = object.getString("baseUrl");
        QueryHelper.logEntry("PROJECT: " + project, project, Area.STARTER.label);

        if (Helper.isThing(project)) {
            QueryHelper.logEntry("PROJECT: " + project, project, Area.ATTENTION.label);
            object.put("project", project);
            object.put("projectId", project);
            object.put(RUN_ID, runId);
            object.put("scenarioId", scenarioId);
            var saved = TestHelper.saveTest(object.getJSONObject("tests"),
                    TestHelper.getSameLevelProject(TEST_PROJECT) + DEFAULT_FEATURE);
            result.put("statusSaved", saved);
            var pageSaved = TestHelper.savePages(object.getJSONObject("config"),
                    TestHelper.getSameLevelProject(TEST_PROJECT) + PAGES);
            result.put("pagesSaved", pageSaved);
            result.put("dashboardSaved", QueryHelper.saveDashboard(object));
            result.put("projectObject", object);
            List<String> map = new ArrayList<>();
            map.add("baseUrl=" + url);
            map.add("manager.url=http://localhost:8082/html");
            map.add("project=" + project);
            map.add("runid=" + runId);
            map.add("scenarioid=" + scenarioId);
            map.add("backend.url=http://localhost:8082/");
            map.add("hostname=109.228.57.213");
            map.add("port=4444");
            var configSaved = TestHelper.saveConfig(map, TestHelper.getSameLevelProject(TEST_PROJECT) + CONFIG);
            QueryHelper.logEntry("CONFIG SAVED: " + configSaved, project, "starter");
            result.put("configSaved", configSaved);

            var runner = new Runner();
            runner.start();

//                var output = CommandRunner.runCommand();
//                System.out.print("RAW OUTPUT: " + output);
//                TestHelper.sleep(2000);
            var sendableOut = "";
//                var sendableOut = output;
//                System.out.println();
//                System.out.println(output.indexOf("OpenJDK"));
//                System.out.println(output.indexOf("Skipped: "));
//
//                if (output.contains("Skipped: ") && output.contains("OpenJDK")) {
//
//                    sendableOut = output
//                            .substring(output.indexOf("OpenJDK"));
//                    sendableOut = sendableOut.substring(0, sendableOut.indexOf("Skipped: "));
//
//                } else {
//                    sendableOut = "CORRUPTED: " + output;
//
//                }
            result.put("output", sendableOut);
            QueryHelper.logEntry(sendableOut, project, Area.CUBE.label);
            LOGGER.info("output");

        } else {
            QueryHelper.logEntry("unauthorized request", project, Area.STARTER.label);
            response.setStatus(401);
        }


        return result.toString();
    }

}
