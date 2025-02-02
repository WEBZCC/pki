// --- BEGIN COPYRIGHT BLOCK ---
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// (C) 2007 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---
package com.netscape.cms.jobs;

import java.io.IOException;
import java.util.Hashtable;

import org.mozilla.jss.netscape.security.x509.X509CertImpl;

import com.netscape.certsrv.base.EBaseException;
import com.netscape.certsrv.base.IConfigStore;
import com.netscape.certsrv.base.ISubsystem;
import com.netscape.certsrv.jobs.IJob;
import com.netscape.certsrv.jobs.IJobCron;
import com.netscape.certsrv.notification.ENotificationException;
import com.netscape.certsrv.notification.IEmailFormProcessor;
import com.netscape.certsrv.notification.IMailNotification;
import com.netscape.certsrv.request.IRequest;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmscore.apps.CMSEngine;
import com.netscape.cmscore.notification.EmailTemplate;
import com.netscape.cmscore.request.Request;

/**
 * This abstract class is a base job for real job extentions for the
 * Jobs Scheduler.
 *
 * @version $Revision$, $Date$
 * @see com.netscape.certsrv.jobs.IJob
 */
public abstract class AJobBase implements IJob, Runnable {

    public static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AJobBase.class);

    // config parameters...
    protected static final String PROP_SUMMARY = "summary";
    protected static final String PROP_ENABLED = "enabled";
    protected static final String PROP_EMAIL_SUBJECT = "emailSubject";
    protected static final String PROP_EMAIL_TEMPLATE = "emailTemplate";
    protected static final String PROP_ITEM_TEMPLATE = "itemTemplate";
    protected static final String PROP_SENDER_EMAIL = "senderEmail";
    protected static final String PROP_RECEIVER_EMAIL = "recipientEmail";

    protected static final String STATUS_FAILURE = "failed";
    protected static final String STATUS_SUCCESS = "succeeded";

    // variables used by the Job Scheduler Daemon
    protected String mImplName = null;
    protected IConfigStore mConfig;
    protected String mId = null;
    protected String mCron = null;
    protected IJobCron mJobCron = null;

    protected static String[] mConfigParams = null;

    protected String mSummaryMailSubject = null;
    protected boolean mMailHTML = false;
    protected String mMailForm = null;
    protected String mItemForm = null;
    protected String mSummarySenderEmail = null;
    protected String mSummaryReceiverEmail = null;
    protected Hashtable<String, Object> mContentParams = new Hashtable<>();
    protected Hashtable<String, Object> mItemParams = new Hashtable<>();

    boolean stopped;

    public AJobBase() {
    }

    /**
     * tells if the job is enabled
     *
     * @return a boolean value indicating whether the job is enabled
     *         or not
     */
    @Override
    public boolean isEnabled() {
        boolean enabled = false;

        try {
            enabled = mConfig.getBoolean(PROP_ENABLED, false);
        } catch (EBaseException e) {
        }
        return enabled;
    }

    /***********************
     * abstract methods
     ***********************/
    @Override
    public abstract void init(ISubsystem owner, String id, String implName, IConfigStore
            config) throws EBaseException;

    @Override
    public abstract void run();

    /***********************
     * public methods
     ***********************/

    /**
     * get instance id.
     *
     * @return a String identifier
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * set instance id.
     *
     * @param id String id of the instance
     */
    @Override
    public void setId(String id) {
        mId = id;
    }

    /**
     * get cron string associated with this job
     *
     * @return a JobCron object that represents the schedule of this job
     */
    @Override
    public IJobCron getJobCron() {
        return mJobCron;
    }

    /**
     * gets the plugin name of this job.
     *
     * @return a String that is the name of this implementation
     */
    @Override
    public String getImplName() {
        return mImplName;
    }

    /**
     * Gets the configuration substore used by this job
     *
     * @return configuration store
     */
    @Override
    public IConfigStore getConfigStore() {
        return mConfig;
    }

    /*
     * get form file content from disk
     */
    protected String getTemplateContent(String templatePath) {
        String templateString = null;

        /*
         * get template file from disk
         */
        EmailTemplate template = new EmailTemplate(templatePath);

        if (!template.init()) {
            logger.warn("AJobBase: " + CMS.getLogMessage("JOBS_TEMPLATE_INIT_ERROR"));
            return null;
        }

        // this should take care of inner tempaltes not being html
        // we go with the outter template
        if (template.isHTML()) {
            mMailHTML = true;
        }
        templateString = template.toString();

        return templateString;
    }

    protected void mailSummary(String content) {
        // no need for email resolver here...
        CMSEngine engine = CMS.getCMSEngine();
        IMailNotification mn = engine.getMailNotification();

        mn.setFrom(mSummarySenderEmail);
        mn.setTo(mSummaryReceiverEmail);
        mn.setSubject(mSummaryMailSubject);
        if (mMailHTML == true) {
            mn.setContentType("text/html");
        }

        mn.setContent(content);
        try {
            mn.sendNotification();
        } catch (ENotificationException e) {
            logger.warn("AJobBase: " + CMS.getLogMessage("JOBS_SEND_NOTIFICATION", e.toString()), e);
        } catch (IOException e) {
            logger.warn("AJobBase: " + CMS.getLogMessage("JOBS_SEND_NOTIFICATION", e.toString()), e);
        }
    }

    protected void buildItemParams(X509CertImpl cert) {
        mItemParams.put(IEmailFormProcessor.TOKEN_SERIAL_NUM,
                cert.getSerialNumber().toString());
        mItemParams.put(IEmailFormProcessor.TOKEN_HEX_SERIAL_NUM,
                cert.getSerialNumber().toString(16));
        mItemParams.put(IEmailFormProcessor.TOKEN_ISSUER_DN,
                cert.getIssuerDN().toString());
        mItemParams.put(IEmailFormProcessor.TOKEN_SUBJECT_DN,
                cert.getSubjectDN().toString());
        mItemParams.put(IEmailFormProcessor.TOKEN_NOT_AFTER,
                cert.getNotAfter().toString());
        mItemParams.put(IEmailFormProcessor.TOKEN_NOT_BEFORE,
                cert.getNotBefore().toString());
        // ... and more
    }

    protected void buildItemParams(IRequest r) {
        String re = r.getExtDataInString(Request.HTTP_PARAMS, "csrRequestorEmail");

        if (re != null) {
            mItemParams.put(IEmailFormProcessor.TOKEN_REQUESTOR_EMAIL, re);
        }

        String ct = r.getExtDataInString(Request.HTTP_PARAMS, Request.CERT_TYPE);

        if (ct != null) {
            mItemParams.put(IEmailFormProcessor.TOKEN_CERT_TYPE, ct);
        }

        String rt = r.getExtDataInString(Request.REQ_TYPE);

        if (rt != null) {
            mItemParams.put(IEmailFormProcessor.TOKEN_REQUEST_TYPE, rt);
        }
    }

    protected void buildItemParams(String name, String val) {
        if (val != null)
            mItemParams.put(name, val);
        else {
            logger.debug("AJobBase: buildItemParams: null value for name= " + name);
            mItemParams.put(name, "");
        }
    }

    protected void buildContentParams(String name, String val) {
        if (val != null)
            mContentParams.put(name, val);
        else {
            logger.debug("AJobBase: buildContentParams: null value for name= " + name);
            mContentParams.put(name, "");
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }
}
