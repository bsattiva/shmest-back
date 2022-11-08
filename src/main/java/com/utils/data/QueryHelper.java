package com.utils.data;


import com.utils.Helper;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import java.util.HashMap;


public class QueryHelper {
    private static final String SECRET = "82hj 36j !!!4 %$ suyseuweyb ^^8hscvb";
    private static final String MAIL_SECRET = "qdb82qjd!^&shaagsa ashjjsag &^(";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String PULL_STRING = "pull-string";
    private static final String EXECUTE = "execute";
    private static final String SUCCESS = "success";
    private final static Logger LOGGER = Logger.getLogger(QueryHelper.class);
    private final static String QUESTION_MASK = "\\?";
    private static final String PROJECT_ID = "projectId";
    private static final String SCENARIO_ID = "scenarioId";
    private static final String RUN_ID = "runId";
    private static final String STEP_ID = "stepId";
    private static final int MAX_RECORDS = 20;
    public static JSONObject getData(final String query,final String type){
        JSONObject object = new JSONObject("{}");
        object.put("query",query);
        object.put("type",type);
        object.put("secret",SECRET);
        String url = Helper.getStringFromProperties(Helper.getHomeDir(new String[]{"config.properties"}),"data.url");
        return HttpClient.sendHttpsPost(object,url);
    }

    public static JSONObject postData(final String url, final JSONObject object){

        return HttpClient.sendHttpsPost(object,url);
    }


    public static JSONObject getProfile(final String token){
        JSONObject result = Helper.getFailedObject();
        String url = Helper.getUrl("backend.url") + "name?token=" + token + "&scope=";
        try {
            result = HttpClient.sendGet(url,new HashMap<>()).getJSONObject("profile");
            result.remove("seed");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getStepStatus(final String runId, final String stepId, final String projectId) {
        final String query = "select r.status from shmest.run r join shmest.dashboard d on r.runid=d.runid where r.runid='?' and r.stepid='?' and d.projectid='?'";
        var arguments = new String[] {runId, stepId, projectId};
        System.out.println("CHECKING STATUS WITH QUERY");
        System.out.println(Helper.completeString(QUESTION_MASK, query, arguments));
        var result = getData(Helper.completeString(QUESTION_MASK, query, arguments), PULL_STRING);
        return (result.has(MESSAGE))
                ? result.getString(MESSAGE) : "-1";

    }
    public static int getDashCount(final String projectId, final String scenarioId) {

        var query = "select count(*) from shmest.dashboard where projectid='?' and scenarioid='?'";
        var countString = getData(Helper.completeString(QUESTION_MASK,
                query,
                new String[] {projectId, scenarioId}), PULL_STRING).getString(MESSAGE);
        return (Helper.isInteger(countString)) ? Integer.parseInt(countString) : 0;
    }

    private static int getDashboardSec(final String projectId, final String scenarioId) {
        var seqQuery = "select max(seq) from shmest.dashboard where projectid='?' and scenarioid='?'";
        var sec = getData(Helper.completeString(QUESTION_MASK, seqQuery, new String[]{projectId, scenarioId}),
                PULL_STRING);
        var seqString = (sec.has(MESSAGE)) ? sec.getString(MESSAGE) : "";
        return (Helper.isInteger(seqString)) ? Integer.parseInt(seqString) : -1;
    }

    private static boolean cleanDashboard(final String projectId, final String scenarioId, final int sec) {
        var count = getDashCount(projectId, scenarioId);
        var status = true;
        if (count > MAX_RECORDS - 1) {
            var delSec= sec - MAX_RECORDS;
            var query = "delete from shmest.dashboard where projectid='?' and scenarioid='?' and sec=?";
            var arguments = new String[] {projectId, scenarioId, Integer.toString(delSec)};
            var message = getData(Helper.completeString(QUESTION_MASK, query, arguments), EXECUTE)
                    .getString(MESSAGE);
            if (!Helper.isThing(message) || !message.equals(SUCCESS)) {
                status = false;
            }
        }
        return status;
    }

    public static boolean saveDashboard(final JSONObject object) {
        var query = "insert into shmest.dashboard(projectid,scenarioid,runid,seq) values('?','?','?',?)";
        var projectId = object.getString(PROJECT_ID);
        var scenarioId = object.getString(SCENARIO_ID);
        var runId = object.getString(RUN_ID);
        var sec = getDashboardSec(projectId, scenarioId);
        var arguments = new String[] {projectId, scenarioId, runId, Integer.toString(sec + 1)};
        var saved = getData(Helper.completeString(QUESTION_MASK, query, arguments), EXECUTE);
        return saved.has(MESSAGE) && saved.getString(MESSAGE).equalsIgnoreCase("success")
                && cleanDashboard(projectId, scenarioId, sec);

    }

    private static int getRunStepSequence(final String runid) {
        var seqQuery = "select max(seq) from shmest.run where runid='" + runid + "'";
        var data = getData(seqQuery, PULL_STRING);
        return (data.length() > 0 && data.has(MESSAGE) && Helper.isInteger(data.getString(MESSAGE)))
                ? (Integer.parseInt(data.getString(MESSAGE)) + 1) : 0;
    }

    public static boolean saveRunStep(final JSONObject object) {
        var status = (object.getString(STATUS).equalsIgnoreCase("passed")) ? 1 : 0;
        var scenarioId = object.getString(SCENARIO_ID);
        var runid = object.getString(RUN_ID);
        var stepId = object.getString(STEP_ID);

            var query = "insert into shmest.run(runid,scenarioid,stepid,seq,status) values('?','?','?',?,?)";
            var saveQuery = Helper
                    .completeString("(\\?)",
                            query,
                            new String[] {runid,
                                    scenarioId,
                                    stepId,
                                    Integer.toString(getRunStepSequence(runid)),
                                    Integer.toString(status)});
            System.out.println(saveQuery);
            var saved = QueryHelper
                    .getData(saveQuery, EXECUTE);



            return saved.has(MESSAGE) && saved.getString(MESSAGE).equalsIgnoreCase("success");
    }



    public static JSONObject persistPage(final JSONObject page) {
        final String pageName = page.getString("pageName");
        final String pageObject = page.getJSONObject("elements").toString().replace("'", "\\\\'");
        final String project = page.getString("project");
        final String url = page.getString("url");
        var status = new JSONObject();
        final String checkQuery = "select pagename from shmest.pages where pagename='"
                + pageName
                + "' and projectid='" + project + "'";
        if (getData(checkQuery, PULL_STRING).length() == 0) {
            final String query = "insert into shmest.pages(pagename,projectid,page,url) values('?','?','?','?')"
                    .replaceFirst(QUESTION_MASK, pageName)
                    .replaceFirst(QUESTION_MASK, project)
                    .replaceFirst(QUESTION_MASK, pageObject)
                    .replaceFirst(QUESTION_MASK, url);

            JSONObject object = new JSONObject("{}");
            object.put("query", query);
            object.put("type", EXECUTE);
            object.put("secret",SECRET);
            status = getData(query, EXECUTE);
        } else {
            final String updateQuery = "update shmest.pages set page='?',url='?' where projectid='?' and pagename='?'"
                    .replaceFirst(QUESTION_MASK, pageObject)
                    .replaceFirst(QUESTION_MASK, url)
                    .replaceFirst(QUESTION_MASK, project)
                    .replaceFirst(QUESTION_MASK, pageName);
            status = getData(updateQuery, EXECUTE);

        }
        return status;
    }


    public static String getProject(final String token){
        String result = "";
        String url = Helper.getUrl("backend.url") + "middle/auth?token=" + token;
        try {
            var res = HttpClient.sendHttpsPost(new JSONObject(), url).getJSONObject("profile");
            result = res.getJSONObject("profile").getJSONObject(MESSAGE).getString("name");

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    public static String getIdByName(final String name){
        String result = "";
        String url = Helper.getUrl("user.url") + "user-id?name=" + name;
        try {
            result = HttpClient.sendGet(url,new HashMap<>()).getString(MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static JSONObject sendMail(String template,String email, String subject,String replace){
        JSONObject result = Helper.getFailedObject();
        JSONObject data = new JSONObject();
        data.put("template",template);
        data.put("email",email);
        data.put("subject",subject);
        data.put("subject",replace);
        String url = Helper.getUrl("mail.url");
        result = HttpClient.sendHttpsPost(data,url);
        return result;
    }

}
