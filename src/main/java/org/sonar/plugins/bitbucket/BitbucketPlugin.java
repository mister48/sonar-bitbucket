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

import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

@Properties({
  @Property(
    key = BitbucketPlugin.BITBUCKET_ENDPOINT,
    defaultValue = "https://api.bitbucket.com",
    name = "Bitbucket API Endpoint",
    description = "URL to access Bitbucket WS API. Deafult value is fine for public Bitbucket. Can be modified for Bitbucket enterprise.",
    global = true),
  @Property(
    key = BitbucketPlugin.BITBUCKET_OAUTH,
    name = "Bitbucket OAuth token",
    description = "Authentication token",
    global = false,
    type = PropertyType.PASSWORD),
  @Property(
    key = BitbucketPlugin.BITBUCKET_REPO,
    name = "Bitbucket repository",
    description = "Bitbucket repository for this project. Will be guessed from '" + CoreProperties.LINKS_SOURCES_DEV + "' if present",
    project = false,
    global = false),
  @Property(
    key = BitbucketPlugin.BITBUCKET_PULL_REQUEST,
    name = "Bitbucket Pull Request",
    description = "Pull request number",
    project = false,
    module = false,
    global = false,
    type = PropertyType.INTEGER),
  @Property(
    key = BitbucketPlugin.BITBUCKET_DISABLE_INLINE_COMMENTS,
    defaultValue = "false",
    name = "Disable issue reporting as inline comments",
    description = "Issues will not be reported as inline comments but only in the global summary comment",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN)
})
public class BitbucketPlugin implements Plugin {

  public static final String BITBUCKET_ENDPOINT = "sonar.bitbucket.endpoint";
  public static final String BITBUCKET_OAUTH = "sonar.bitbucket.oauth";
  public static final String BITBUCKET_REPO = "sonar.bitbucket.repository";
  public static final String BITBUCKET_PULL_REQUEST = "sonar.bitbucket.pullRequest";
  public static final String BITBUCKET_DISABLE_INLINE_COMMENTS = "sonar.bitbucket.disableInlineComments";

  @Override
  public void define(Context context) {
    context.addExtensions(
      PullRequestIssuePostJob.class,
      BitbucketPluginConfiguration.class,
      PullRequestProjectBuilder.class,
      PullRequestFacade.class,
      MarkDownUtils.class);
  }

}
