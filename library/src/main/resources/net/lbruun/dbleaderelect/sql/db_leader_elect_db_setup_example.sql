-- 
-- Copyright 2021 lbruun.
-- 
--  Licensed under the Apache License, Version 2.0 (the "License");
--  you may not use this file except in compliance with the License.
--  You may obtain a copy of the License at
-- 
--       http://www.apache.org/licenses/LICENSE-2.0
-- 
--  Unless required by applicable law or agreed to in writing, software
--  distributed under the License is distributed on an "AS IS" BASIS,
--  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--  See the License for the specific language governing permissions and
--  limitations under the License.
-- 
--  -------------------------------------------------------------
--
--
--
--  Creates and populate the table needed by DbLeaderElect.
--

CREATE TABLE db_leader_elect 
 (
     role_id               varchar(20)    NOT NULL,
     candidate_id          varchar(256)   NOT NULL,
     last_seen_timestamp   bigint         NOT NULL,
     lease_counter         bigint         NOT NULL
  );

ALTER TABLE db_leader_elect
    ADD CONSTRAINT pk_db_leader_elect PRIMARY KEY (role_id);

INSERT INTO db_leader_elect   VALUES ('DEFAULT', '//noleader//', 0, 0);
