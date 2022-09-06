/*
 * Copyright 2022 lbruun.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.dbleaderelection.springboot.autoconfigure;

import javax.sql.DataSource;
import net.lbruun.dbleaderelect.LeaderElector;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.spring.SpringLeaderElectorProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for DbLeaderElection.
 */

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({LeaderElectorListener.class, DataSource.class})
@ConditionalOnMissingBean({LeaderElector.class})
@AutoConfigureAfter({DataSourceAutoConfiguration.class, LiquibaseAutoConfiguration.class})
public class DbLeaderElectionAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix="dbleaderelection")
    public SpringLeaderElectorProperties SpringLeaderElectorProperties() {
        return new SpringLeaderElectorProperties();
    }
    
    @Bean
    public LeaderElector leaderElector(
            SpringLeaderElectorProperties properties,
            LeaderElectorListener LeaderElectorListener,
            DataSourceProperties dataSourceProperties,
            ObjectProvider<DataSource> dataSourceObjectProvider,
            @LeaderElectorDataSource ObjectProvider<DataSource> leaderElectorDataSourceObjectProvider) {
        
        DataSource ds = leaderElectorDataSourceObjectProvider.getIfAvailable();
        if (ds == null) {
            ds = dataSourceObjectProvider.getIfUnique();
        }

        if (ds == null) {
            throw new RuntimeException("Multiple beans of type DataSource exist. Cannot figure out which one to use");
        }

        LeaderElectorConfiguration c = properties.configuration(LeaderElectorListener);
        return new LeaderElector(c, ds);
    }

}

