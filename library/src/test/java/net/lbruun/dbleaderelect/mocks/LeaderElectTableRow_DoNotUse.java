/*
 * Copyright 2022 lbruun.
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
package net.lbruun.dbleaderelect.mocks;

public class LeaderElectTableRow_DoNotUse {

    private final String nodeId;
    private final long lastSeenTimestamp;
    private final long nowUtcMs;

    public LeaderElectTableRow_DoNotUse(String nodeId, long lastSeenTimestamp, long nowUtcMs) {
        this.nodeId = nodeId;
        this.lastSeenTimestamp = lastSeenTimestamp;
        this.nowUtcMs = nowUtcMs;
    }

    public String getNodeId() {
        return nodeId;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public long getNowUtcMs() {
        return nowUtcMs;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String nodeId;
        private long lastSeenTimestamp;
        private long nowUtcMs;

        public Builder() {
        }

        public Builder withNodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder withLastSeenTimestamp(long lastSeenTimestamp) {
            this.lastSeenTimestamp = lastSeenTimestamp;
            return this;
        }

        public Builder withNowUtcMs(long nowUtcMs) {
            this.nowUtcMs = nowUtcMs;
            return this;
        }

        public LeaderElectTableRow_DoNotUse build() {
            return new LeaderElectTableRow_DoNotUse(nodeId, lastSeenTimestamp, nowUtcMs);
        }
    }
}
