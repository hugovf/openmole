/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.implementation.execution.batch;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchJobService;
import org.openmole.core.model.execution.batch.IBatchStorage;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.implementation.execution.Environment;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.model.execution.batch.IBatchServiceGroup;
import org.openmole.core.model.job.IJob;
import org.openmole.misc.workspace.InteractiveConfiguration;
import scala.Tuple2;

public abstract class BatchEnvironment<JS extends IBatchJobService> extends Environment<IBatchExecutionJob> implements IBatchEnvironment<JS> {

    final static String ConfigGroup = BatchEnvironment.class.getSimpleName();
    final static ConfigurationLocation MemorySizeForRuntime = new ConfigurationLocation(ConfigGroup, "MemorySizeForRuntime");
    @InteractiveConfiguration(label = "Runtime location")
    final static ConfigurationLocation RuntimeLocation = new ConfigurationLocation(ConfigGroup, "RuntimeLocation");
    final static ConfigurationLocation ResourcesExpulseThreshod = new ConfigurationLocation(ConfigGroup, "ResourcesExpulseThreshod");

    static {
        Activator.getWorkspace().addToConfigurations(MemorySizeForRuntime, "512");
        Activator.getWorkspace().addToConfigurations(ResourcesExpulseThreshod, "20");
    }
    
    transient BatchServiceGroup<JS> jobServices;
    transient BatchServiceGroup<IBatchStorage> storages;
    transient Lock initJS;
    transient Lock initST;
    
    final Integer memorySizeForRuntime;
    final File runtime;

    public BatchEnvironment(int memorySizeForRuntime) throws InternalProcessingError {
        super();
     
        this.memorySizeForRuntime = memorySizeForRuntime;
        this.runtime = new File(Activator.getWorkspace().getPreference(RuntimeLocation));

        Activator.getUpdater().registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN, Activator.getWorkspace().getPreferenceAsDurationInMs(BatchJobWatcher.CheckInterval));
    }

    public BatchEnvironment() throws InternalProcessingError {
        this(Activator.getWorkspace().getPreferenceAsInt(MemorySizeForRuntime));
    }

    @Override
    public void submit(IJob job) throws InternalProcessingError, UserBadDataError {
        final BatchExecutionJob<JS> bej = new BatchExecutionJob<JS>(this, job);

        Activator.getUpdater().delay(bej, ExecutorType.UPDATE);

        getJobRegistry().register(bej);
    }

    @Override
    public synchronized File getRuntime() {
        return runtime;
    }

    public Integer getMemorySizeForRuntime() {
        return memorySizeForRuntime;
    }

    protected BatchServiceGroup<IBatchStorage> selectStorages() throws InternalProcessingError, UserBadDataError, InterruptedException {
        final BatchServiceGroup<IBatchStorage> storages = new BatchServiceGroup<IBatchStorage>(Activator.getWorkspace().getPreferenceAsInt(ResourcesExpulseThreshod));

        Collection<IBatchStorage> stors = allStorages();

        final Semaphore oneFinished = new Semaphore(0);
        final AtomicInteger nbLeftRunning = new AtomicInteger(stors.size());

        for (final IBatchStorage storage : stors) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        if (storage.test()) {
                            storages.put(storage);
                        }
                    } finally {
                        nbLeftRunning.decrementAndGet();
                        oneFinished.release();
                    }
                }
            };

            Activator.getExecutorService().getExecutorService(ExecutorType.OWN).submit(r);
        }

        while ((storages.isEmpty()) && nbLeftRunning.get() > 0) {
            oneFinished.acquire();
        }

        if (storages.isEmpty()) {
            throw new InternalProcessingError("No storage available");
        }
        return storages;
    }

    protected BatchServiceGroup<JS> selectWorkingJobServices() throws InternalProcessingError, UserBadDataError, InterruptedException {
        final BatchServiceGroup<JS> jobServices = new BatchServiceGroup<JS>(Activator.getWorkspace().getPreferenceAsInt(ResourcesExpulseThreshod));
        Collection<JS> allJobServices = allJobServices();
        final Semaphore done = new Semaphore(0);
        final AtomicInteger nbStillRunning = new AtomicInteger(allJobServices.size());

        for (final JS js : allJobServices) {

            Runnable test = new Runnable() {

                @Override
                public void run() {
                    try {
                        if (js.test()) {
                            jobServices.put(js);
                        }
                    } finally {
                        nbStillRunning.decrementAndGet();
                        done.release();
                    }
                }
            };

            Activator.getExecutorService().getExecutorService(ExecutorType.OWN).submit(test);
        }


        while (jobServices.isEmpty() && nbStillRunning.get() > 0) {
            done.acquire();
        }

        if (jobServices.isEmpty()) {
            throw new InternalProcessingError("No job submission service available");
        }

        return jobServices;
    }

    @Override
    public Tuple2<IBatchStorage, IAccessToken> getAStorage() throws InternalProcessingError, UserBadDataError, InterruptedException {
        return getStorages().getAService();
    }

    @Override
    public IBatchServiceGroup<JS> getJobServices() throws InternalProcessingError, UserBadDataError, InterruptedException {

        getInitJS().lock();
        try {
            if (jobServices == null || jobServices.isEmpty()) {
                jobServices = selectWorkingJobServices();
            }
            return jobServices;
        } finally {
            getInitJS().unlock();
        }
    }

    @Override
    public IBatchServiceGroup<IBatchStorage> getStorages() throws InternalProcessingError, UserBadDataError, InterruptedException {
        getInitST().lock();

        try {
            if (storages == null || storages.isEmpty()) {
                storages = selectStorages();
            }
            return storages;
        } finally {
            getInitST().unlock();
        }
    }

    @Override
    public Tuple2<JS, IAccessToken> getAJobService() throws InternalProcessingError, UserBadDataError, InterruptedException {
        return getJobServices().getAService();
    }

    private Lock getInitJS() {
        if (initJS != null) {
            return initJS;
        }

        synchronized (this) {
            if (initJS == null) {
                initJS = new ReentrantLock();
            }
            return initJS;
        }
    }

    private Lock getInitST() {
        if (initST != null) {
            return initST;
        }

        synchronized (this) {
            if (initST == null) {
                initST = new ReentrantLock();
            }
            return initST;
        }
    }
}
