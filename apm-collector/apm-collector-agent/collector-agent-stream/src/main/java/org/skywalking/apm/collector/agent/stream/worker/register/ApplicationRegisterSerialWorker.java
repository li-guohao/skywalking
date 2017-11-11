/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.agent.stream.worker.register;

import org.skywalking.apm.collector.agent.stream.IdAutoIncrement;
import org.skywalking.apm.collector.cache.CacheServiceManager;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.queue.service.QueueCreatorService;
import org.skywalking.apm.collector.storage.dao.IApplicationStreamDAO;
import org.skywalking.apm.collector.storage.service.DAOService;
import org.skywalking.apm.collector.storage.table.register.Application;
import org.skywalking.apm.collector.stream.worker.base.AbstractLocalAsyncWorker;
import org.skywalking.apm.collector.stream.worker.base.AbstractLocalAsyncWorkerProvider;
import org.skywalking.apm.collector.stream.worker.base.WorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class ApplicationRegisterSerialWorker extends AbstractLocalAsyncWorker<Application, Application> {

    private final Logger logger = LoggerFactory.getLogger(ApplicationRegisterSerialWorker.class);

    public ApplicationRegisterSerialWorker(DAOService daoService, CacheServiceManager cacheServiceManager) {
        super(daoService, cacheServiceManager);
    }

    @Override public int id() {
        return 0;
    }

    @Override protected void onWork(Application application) throws WorkerException {
        logger.debug("register application, application code: {}", application.getApplicationCode());
        int applicationId = getCacheServiceManager().getApplicationCacheService().get(application.getApplicationCode());

        if (applicationId == 0) {
            IApplicationStreamDAO dao = (IApplicationStreamDAO)getDaoService().get(IApplicationStreamDAO.class);
            int min = dao.getMinApplicationId();
            if (min == 0) {
                Application userApplication = new Application(String.valueOf(Const.USER_ID));
                userApplication.setApplicationCode(Const.USER_CODE);
                userApplication.setApplicationId(Const.USER_ID);
                dao.save(userApplication);

                application = new Application("-1");
                application.setApplicationId(-1);
            } else {
                int max = dao.getMaxApplicationId();
                applicationId = IdAutoIncrement.INSTANCE.increment(min, max);

                application = new Application(String.valueOf(applicationId));
                application.setApplicationId(applicationId);
            }
            dao.save(application);
        }
    }

    public static class Factory extends AbstractLocalAsyncWorkerProvider<Application, Application, ApplicationRegisterSerialWorker> {

        public Factory(DAOService daoService, CacheServiceManager cacheServiceManager,
            QueueCreatorService<Application> queueCreatorService) {
            super(daoService, cacheServiceManager, queueCreatorService);
        }

        @Override public ApplicationRegisterSerialWorker workerInstance(DAOService daoService,
            CacheServiceManager cacheServiceManager) {
            return new ApplicationRegisterSerialWorker(daoService, cacheServiceManager);
        }

        @Override public int queueSize() {
            return 256;
        }
    }
}
