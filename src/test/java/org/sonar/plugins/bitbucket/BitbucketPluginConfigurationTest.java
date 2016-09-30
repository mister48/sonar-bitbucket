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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;

public class BitbucketPluginConfigurationTest {

  private Settings settings;
  private BitbucketPluginConfiguration config;

  @Before
  public void prepare() {
    settings = new Settings(new PropertyDefinitions(BitbucketPlugin.class));
    config = new BitbucketPluginConfiguration(settings);
  }

  @Test
  public void guessRepositoryFromScmUrl() {
    try {
      config.repository();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class)
        .hasMessage("Unable to determine Bitbucket repository name for this project. Please provide it using property '" + BitbucketPlugin.BITBUCKET_REPO
          + "' or configure property '" + CoreProperties.LINKS_SOURCES + "'.");
    }

    settings.setProperty(CoreProperties.LINKS_SOURCES, "do_not_match_1");
    try {
      config.repository();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class)
        .hasMessage("Unable to parse Bitbucket repository name for this project. Please check configuration:\n  * " + CoreProperties.LINKS_SOURCES_DEV
          + ": null\n  * " + CoreProperties.LINKS_SOURCES + ": do_not_match_1");
    }
    settings.clear();
    settings.setProperty(CoreProperties.LINKS_SOURCES_DEV, "do_not_match_2");
    try {
      config.repository();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class)
        .hasMessage("Unable to parse Bitbucket repository name for this project. Please check configuration:\n  * " + CoreProperties.LINKS_SOURCES_DEV
          + ": do_not_match_2\n  * " + CoreProperties.LINKS_SOURCES + ": null");
    }

    settings.clear();
    settings.setProperty(CoreProperties.LINKS_SOURCES, "scm:git:git@bitbucket.com:SonarCommunity/bitbucket-integration.git");
    assertThat(config.repository()).isEqualTo("SonarCommunity/bitbucket-integration");

    settings.setProperty(CoreProperties.LINKS_SOURCES_DEV, "do_not_parse");
    assertThat(config.repository()).isEqualTo("SonarCommunity/bitbucket-integration");

    settings.setProperty(CoreProperties.LINKS_SOURCES_DEV, "scm:git:git@bitbucket.com:SonarCommunity2/bitbucket-integration.git");
    assertThat(config.repository()).isEqualTo("SonarCommunity2/bitbucket-integration");

    settings.removeProperty(CoreProperties.LINKS_SOURCES);
    assertThat(config.repository()).isEqualTo("SonarCommunity2/bitbucket-integration");

    settings.setProperty(BitbucketPlugin.BITBUCKET_REPO, "https://bitbucket.com/SonarCommunity/sonar-bitbucket.git");
    assertThat(config.repository()).isEqualTo("SonarCommunity/sonar-bitbucket");
    settings.setProperty(BitbucketPlugin.BITBUCKET_REPO, "http://bitbucket.com/SonarCommunity/sonar-bitbucket.git");
    assertThat(config.repository()).isEqualTo("SonarCommunity/sonar-bitbucket");
    settings.setProperty(BitbucketPlugin.BITBUCKET_REPO, "SonarCommunity3/bitbucket-integration");
    assertThat(config.repository()).isEqualTo("SonarCommunity3/bitbucket-integration");
  }

  @Test
  public void other() {
    settings.setProperty(BitbucketPlugin.BITBUCKET_OAUTH, "oauth");
    assertThat(config.oauth()).isEqualTo("oauth");

    assertThat(config.isEnabled()).isFalse();
    settings.setProperty(BitbucketPlugin.BITBUCKET_PULL_REQUEST, "3");
    assertThat(config.pullRequestNumber()).isEqualTo(3);
    assertThat(config.isEnabled()).isTrue();

    assertThat(config.endpoint()).isEqualTo("https://api.bitbucket.com");
    settings.setProperty(BitbucketPlugin.BITBUCKET_ENDPOINT, "http://myprivate-endpoint");
    assertThat(config.endpoint()).isEqualTo("http://myprivate-endpoint");

    assertThat(config.tryReportIssuesInline()).isTrue();
    settings.setProperty(BitbucketPlugin.BITBUCKET_DISABLE_INLINE_COMMENTS, "true");
    assertThat(config.tryReportIssuesInline()).isFalse();
  }

}
