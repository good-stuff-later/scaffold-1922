/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.dbleaderelection.example2.jpa;

import java.time.LocalDate;
import net.lbruun.dbleaderelection.example2.LeaderElectionExampleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Do something on the database to show that DB operations work.
 */
@Component
public class DoDbOperations implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DoDbOperations.class);

    private final PersonRepo personRepo;

    public DoDbOperations(PersonRepo personRepo) {
        this.personRepo = personRepo;
    }

    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (personRepo.count() == 0) {
            Person person = new Person();
            person.setPersonId(1);
            person.setFirstName("Donald");
            person.setLastName("Duck");
            person.setBirthDate(LocalDate.of(1934, 6, 9));
            personRepo.save(person);
            logger.info("Saved record : " + person);
        }
        
        for(Person person : personRepo.findAll()) {
            logger.info("Retrieved from table : " + person);
        }
    }
    
}
