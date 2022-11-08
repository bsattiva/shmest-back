package com.testshmestservice.testshmestservice.controller;

import com.utils.FileHelper;
import com.utils.Helper;
import com.utils.HtmlHelper;
import com.utils.RequestHelper;
import com.utils.TestHelper;
import com.utils.command.CommandRunner;
import com.utils.data.QueryHelper;
import lombok.Getter;
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
        var token = request.getParameter(TOKEN).replace(" ", "+");
        var project = getProject(request);
        var name = request.getParameter("id");
        var query = "select page,url from shmest.pages where projectid='?' and pagename='?'"
                .replaceFirst(QUESTION_MASK, project)
                .replaceFirst(QUESTION_MASK, name);
        var data = QueryHelper.getData(query, "pull-table");

        var object = (data.has("message") && data.getJSONArray("message").length() > 0)
                ? new JSONObject(data.getJSONArray("message").getJSONObject(0).toString()
                .replace(":\"{", ":{")
                .replace("}\",", "},")
                .replace("[\"{","[{")
                .replace("}\"]", "}]")
                .replace("\\", "")
                .replaceAll("\"\"([a-zA-Z0-9]*)\"\"", "\"\"$1\"\"")) : new JSONObject();
        if (object.has("page")) {
            var page = object.getJSONObject("page");
            var keys = page.keys();
            var arr = new JSONArray();
            while (keys.hasNext()) {
                var key = keys.next();
                var obj = page.getJSONObject(key);
                arr.put(obj);
            }
            object.put("page", arr);
        }
        return object.toString();
    }


    @GetMapping("/pages")
    String getPages(final HttpServletRequest request, final HttpServletResponse response) {
        var token = request.getParameter(TOKEN).replace(" ", "+");
        var project = getProject(request);
        var query = "select pagename,page,url from shmest.pages where projectid='?'".replace(MASK, project);
        var data = QueryHelper.getData(query, "pull-table");

        var array =  new JSONArray(data.getJSONArray("message").toString()
                .replace(":\"{", ":{")
                .replace("}\",", "},")
                .replace("[\"{","[{")
                .replace("}\"]", "}]")
                .replace("\\", "")
                .replaceAll("\"\"([a-zA-Z0-9]*)\"\"", "\"\"$1\"\""));
        for (var i = 0; i < array.length(); i++) {
            var object = array.getJSONObject(i);
            var arr = new JSONArray();
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


    @PostMapping("/html")
    @ResponseBody
    String readHtml(final HttpServletRequest request, final HttpServletResponse response) {
        var result = new JSONObject();
        var object = RequestHelper.getRequestBody(request);
        var pageName = object.getString(PAGE_NAME);
        var html = object.getString(HTML);
        var pageObject = HtmlHelper.parseHtml(html);
        result.put(PAGE_NAME, pageName);
        result.put("elements", pageObject);
        result.put("project", object.getString("project"));
        result.put("url", object.getString("url"));
        System.out.print(result.toString(5));
        System.out.print(QueryHelper.persistPage(result));
        return result.toString();
    }

    @PostMapping("/step")
    String saveStep(final HttpServletRequest request, final HttpServletResponse response) {
        var object = RequestHelper.getRequestBody(request);
        var secret = object.getString("secret");
        var result = new JSONObject("{\"message\":{\"status\":\"failure\"}}");
        if (secret.equals(SECRET)) {
            var statObject = new JSONObject();
            statObject.put("status",  QueryHelper.saveRunStep(object));
            result.put("message", statObject);
        } else {
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


    @PostMapping("/start")
    String startTest(final HttpServletRequest request, final HttpServletResponse response) throws java.net.MalformedURLException {
        var result = new JSONObject();

        var runId = Helper.getRandomString(8);
        var object = RequestHelper.getRequestBody(request);
        if(object.has(RUN_ID)) {
            runId = object.getString(RUN_ID);
        }
        var scenarioId = (object.has("scenarioId"))
                ? object.getString("scenarioId") : Helper.getRandomString(10);
        var token = object.getString(TOKEN);
        var project = QueryHelper.getProject(token);
        var url = object.getString("baseUrl");

        if (Helper.isThing(project)) {
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
            var configSaved = TestHelper.saveConfig(map, TestHelper.getSameLevelProject(TEST_PROJECT) + CONFIG);
            result.put("configSaved", configSaved);
            try {
                var output = CommandRunner.runCommand();
                result.put("output", output);
                System.out.print(output);
                LOGGER.info(output);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                result.put("error", e.getMessage());
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }

        } else {
            response.setStatus(401);
        }


        return result.toString();
    }

}
