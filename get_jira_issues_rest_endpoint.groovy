import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

import org.apache.log4j.*
log.level = Level.ALL

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchProvider = ComponentAccessor.getComponent(SearchProvider)
def issueManager = ComponentAccessor.getIssueManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

// edit this query to suit
def query = jqlQueryParser.parseQuery("project = TIS and assignee != \"admin\" and updated <= -7d and resolution is EMPTY")

def results = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter())

log.debug("Total issues: ${results.total}")



import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

// Adding a REST Endpoint
// https://scriptrunner.adaptavist.com/latest/jira/rest-endpoints.html

@BaseScript CustomEndpointDelegate delegate // this line makes methods in your script recognisable as endpoints, and is required

getInactiveIssues(  //  the name of the REST endpoint, which forms part of the URL
    httpMethod: "GET", groups: ["jira-administrators"]  // configuration of the endpoint, in this case which HTTP verb to handle, and what groups to allow
) { MultivaluedMap queryParams, String body ->  // parameters which are provided to your method body
    return Response.ok(new JsonBuilder([inactive_issues_count: results.total]).toString()).build()  // the body of your method, where you will return a javax.ws.rs.core.Response object 
}
