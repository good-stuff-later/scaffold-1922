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
package net.lbruun.dbleaderelection.example2;

import javax.sql.DataSource;
import net.lbruun.dbleaderelect.LeaderElector;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.spring.SpringLeaderElectorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class LeaderElectionExampleConfiguration {
    
    @Bean
    @ConfigurationProperties(prefix="dbleaderelection")
    public SpringLeaderElectorProperties springLeaderElectorProperties() {
        return new SpringLeaderElectorProperties();
    }
    
    @Bean
    public LeaderElectorListener leaderElectorListener() {
        return new LeaderElectionExampleListener();
    }
    
    @Bean
    public LeaderElector leaderElector(
            SpringLeaderElectorProperties props, 
            DataSource dataSource, 
            LeaderElectorListener leaderElectorListener) {
        LeaderElectorConfiguration configuration = props.configuration(leaderElectorListener);
        return new LeaderElector(configuration, dataSource);
    }
}
