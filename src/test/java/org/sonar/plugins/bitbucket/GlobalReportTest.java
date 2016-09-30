/*
 * SonarQube :: Bitbucket Plugin
 * Copyright (C) 2015-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.bitbucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.CheckForNull;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;

public class GlobalReportTest {

  private static final String BITBUCKET_URL = "https://bitbucket.com/SonarCommunity/sonar-bitbucket";

  private Settings settings;

  @Before
  public void setup() {
    settings = new Settings(new PropertyDefinitions(PropertyDefinition.builder(CoreProperties.SERVER_BASE_URL)
      .name("Server base URL")
      .description("HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.")
      .category(CoreProperties.CATEGORY_GENERAL)
      .defaultValue(CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE)
      .build()));

    settings.setProperty("sonar.host.url", "http://myserver");
  }

  private PostJobIssue newMockedIssue(String componentKey, @CheckForNull DefaultInputFile inputFile, @CheckForNull Integer line, Severity severity, boolean isNew, String message,
    String rule) {
    PostJobIssue issue = mock(PostJobIssue.class);
    when(issue.inputComponent()).thenReturn(inputFile);
    when(issue.componentKey()).thenReturn(componentKey);
    if (line != null) {
      when(issue.line()).thenReturn(line);
    }
    when(issue.ruleKey()).thenReturn(RuleKey.of("repo", rule));
    when(issue.severity()).thenReturn(severity);
    when(issue.isNew()).thenReturn(isNew);
    when(issue.message()).thenReturn(message);

    return issue;
  }

  @Test
  public void noIssues() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), true);

    String desiredMarkdown = "SonarQube analysis reported no issues.";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void oneIssue() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), true);
    globalReport.process(newMockedIssue("component", null, null, Severity.INFO, true, "Issue", "rule"), BITBUCKET_URL, true);

    String desiredMarkdown = "SonarQube analysis reported 1 issue\n" +
      "* ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) 1 info\n" +
      "\nWatch the comments in this conversation to review them.\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void oneIssueOnDir() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), true);
    globalReport.process(newMockedIssue("component0", null, null, Severity.INFO, true, "Issue0", "rule0"), null, false);

    String desiredMarkdown = "SonarQube analysis reported 1 issue\n\n" +
      "Note: The following issues were found on lines that were not modified in the pull request. Because these issues can't be reported as line comments, they are summarized here:\n\n"
      +
      "1. ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) component0: Issue0 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule0)\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void shouldFormatIssuesForMarkdownNoInline() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), true);
    globalReport.process(newMockedIssue("component", null, null, Severity.INFO, true, "Issue", "rule"), BITBUCKET_URL, true);
    globalReport.process(newMockedIssue("component", null, null, Severity.MINOR, true, "Issue", "rule"), BITBUCKET_URL, true);
    globalReport.process(newMockedIssue("component", null, null, Severity.MAJOR, true, "Issue", "rule"), BITBUCKET_URL, true);
    globalReport.process(newMockedIssue("component", null, null, Severity.CRITICAL, true, "Issue", "rule"), BITBUCKET_URL, true);
    globalReport.process(newMockedIssue("component", null, null, Severity.BLOCKER, true, "Issue", "rule"), BITBUCKET_URL, true);

    String desiredMarkdown = "SonarQube analysis reported 5 issues\n" +
      "* ![BLOCKER](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-blocker.png) 1 blocker\n" +
      "* ![CRITICAL](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-critical.png) 1 critical\n" +
      "* ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) 1 major\n" +
      "* ![MINOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-minor.png) 1 minor\n" +
      "* ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) 1 info\n" +
      "\nWatch the comments in this conversation to review them.\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void shouldFormatIssuesForMarkdownMixInlineGlobal() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), true);
    globalReport.process(newMockedIssue("component", null, null, Severity.INFO, true, "Issue 0", "rule0"), BITBUCKET_URL, true);
    globalReport.process(newMockedIssue("component", null, null, Severity.MINOR, true, "Issue 1", "rule1"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.MAJOR, true, "Issue 2", "rule2"), BITBUCKET_URL, true);
    globalReport.process(newMockedIssue("component", null, null, Severity.CRITICAL, true, "Issue 3", "rule3"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.BLOCKER, true, "Issue 4", "rule4"), BITBUCKET_URL, true);

    String desiredMarkdown = "SonarQube analysis reported 5 issues\n" +
      "* ![BLOCKER](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-blocker.png) 1 blocker\n" +
      "* ![CRITICAL](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-critical.png) 1 critical\n" +
      "* ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) 1 major\n" +
      "* ![MINOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-minor.png) 1 minor\n" +
      "* ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) 1 info\n" +
      "\nWatch the comments in this conversation to review them.\n" +
      "\n#### 2 extra issues\n" +
      "\nNote: The following issues were found on lines that were not modified in the pull request. Because these issues can't be reported as line comments, they are summarized here:\n\n"
      +
      "1. ![MINOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-minor.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 1 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule1)\n"
      +
      "1. ![CRITICAL](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-critical.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 3 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule3)\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void shouldFormatIssuesForMarkdownWhenInlineCommentsDisabled() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), false);
    globalReport.process(newMockedIssue("component", null, null, Severity.INFO, true, "Issue 0", "rule0"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.MINOR, true, "Issue 1", "rule1"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.MAJOR, true, "Issue 2", "rule2"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.CRITICAL, true, "Issue 3", "rule3"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.BLOCKER, true, "Issue 4", "rule4"), BITBUCKET_URL, false);

    String desiredMarkdown = "SonarQube analysis reported 5 issues\n\n" +
      "1. ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 0 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule0)\n"
      +
      "1. ![MINOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-minor.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 1 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule1)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 2 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule2)\n"
      +
      "1. ![CRITICAL](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-critical.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 3 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule3)\n"
      +
      "1. ![BLOCKER](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-blocker.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 4 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule4)\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void shouldFormatIssuesForMarkdownWhenInlineCommentsDisabledAndLimitReached() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), false, 4);
    globalReport.process(newMockedIssue("component", null, null, Severity.INFO, true, "Issue 0", "rule0"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.MINOR, true, "Issue 1", "rule1"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.MAJOR, true, "Issue 2", "rule2"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.CRITICAL, true, "Issue 3", "rule3"), BITBUCKET_URL, false);
    globalReport.process(newMockedIssue("component", null, null, Severity.BLOCKER, true, "Issue 4", "rule4"), BITBUCKET_URL, false);

    String desiredMarkdown = "SonarQube analysis reported 5 issues\n" +
      "* ![BLOCKER](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-blocker.png) 1 blocker\n" +
      "* ![CRITICAL](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-critical.png) 1 critical\n" +
      "* ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) 1 major\n" +
      "* ![MINOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-minor.png) 1 minor\n" +
      "* ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) 1 info\n" +
      "\n#### Top 4 issues\n\n" +
      "1. ![INFO](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-info.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 0 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule0)\n"
      +
      "1. ![MINOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-minor.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 1 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule1)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 2 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule2)\n"
      +
      "1. ![CRITICAL](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-critical.png) [sonar-bitbucket](https://bitbucket.com/SonarCommunity/sonar-bitbucket): Issue 3 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule3)\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void shouldLimitGlobalIssues() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), true);
    for (int i = 0; i < 17; i++) {
      globalReport.process(newMockedIssue("component", null, null, Severity.MAJOR, true, "Issue number:" + i, "rule" + i), BITBUCKET_URL + "/File.java#L" + i, false);
    }

    String desiredMarkdown = "SonarQube analysis reported 17 issues\n" +
      "* ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) 17 major\n" +
      "\n#### Top 10 extra issues\n" +
      "\nNote: The following issues were found on lines that were not modified in the pull request. Because these issues can't be reported as line comments, they are summarized here:\n\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L0](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L0): Issue number:0 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule0)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L1](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L1): Issue number:1 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule1)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L2](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L2): Issue number:2 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule2)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L3](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L3): Issue number:3 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule3)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L4](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L4): Issue number:4 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule4)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L5](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L5): Issue number:5 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule5)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L6](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L6): Issue number:6 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule6)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L7](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L7): Issue number:7 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule7)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L8](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L8): Issue number:8 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule8)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L9](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L9): Issue number:9 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule9)\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }

  @Test
  public void shouldLimitGlobalIssuesWhenInlineCommentsDisabled() {
    GlobalReport globalReport = new GlobalReport(new MarkDownUtils(settings), false);
    for (int i = 0; i < 17; i++) {
      globalReport.process(newMockedIssue("component", null, null, Severity.MAJOR, true, "Issue number:" + i, "rule" + i), BITBUCKET_URL + "/File.java#L" + i, false);
    }

    String desiredMarkdown = "SonarQube analysis reported 17 issues\n" +
      "* ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) 17 major\n" +
      "\n#### Top 10 issues\n\n" +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L0](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L0): Issue number:0 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule0)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L1](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L1): Issue number:1 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule1)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L2](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L2): Issue number:2 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule2)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L3](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L3): Issue number:3 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule3)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L4](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L4): Issue number:4 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule4)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L5](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L5): Issue number:5 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule5)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L6](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L6): Issue number:6 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule6)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L7](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L7): Issue number:7 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule7)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L8](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L8): Issue number:8 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule8)\n"
      +
      "1. ![MAJOR](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/severity-major.png) [File.java#L9](https://bitbucket.com/SonarCommunity/sonar-bitbucket/File.java#L9): Issue number:9 [![rule](https://raw.bitbucketusercontent.com/SonarCommunity/sonar-bitbucket/master/images/rule.png)](http://myserver/coding_rules#rule_key=repo%3Arule9)\n";

    String formattedGlobalReport = globalReport.formatForMarkdown();

    assertThat(formattedGlobalReport).isEqualTo(desiredMarkdown);
  }
}
