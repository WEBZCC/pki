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
package com.netscape.cms.servlet.ocsp;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dogtagpki.server.authorization.AuthzToken;

import com.netscape.certsrv.authentication.IAuthToken;
import com.netscape.certsrv.authorization.EAuthzAccessDenied;
import com.netscape.certsrv.base.EBaseException;
import com.netscape.certsrv.ocsp.IOCSPService;
import com.netscape.cms.servlet.base.CMSServlet;
import com.netscape.cms.servlet.common.CMSRequest;
import com.netscape.cms.servlet.common.CMSTemplate;
import com.netscape.cms.servlet.common.CMSTemplateParams;
import com.netscape.cms.servlet.common.ECMSGWException;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmscore.base.ArgBlock;

/**
 * Retrieve information about the number of OCSP requests the OCSP
 * has serviced
 *
 * @version $Revision$, $Date$
 */
public class GetOCSPInfo extends CMSServlet {
    /**
     *
     */
    private static final long serialVersionUID = -3633557968127876119L;
    private final static String TPL_FILE = "getOCSPInfo.template";
    private String mFormPath = null;

    public GetOCSPInfo() {
        super();
    }

    /**
     * initialize the servlet. This servlet uses the template
     * file "getOCSPInfo.template" to render the result page.
     *
     * @param sc servlet configuration, read from the web.xml file
     */
    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        // override success to display own output.

        // coming from agent
        mFormPath = "/" + mAuthority.getId() + "/" + TPL_FILE;

        mTemplates.remove(CMSRequest.SUCCESS);
        if (mOutputTemplatePath != null)
            mFormPath = mOutputTemplatePath;

    }

    /**
     * Process the HTTP request.
     *
     * @param cmsReq the object holding the request and response information
     */
    @Override
    protected void process(CMSRequest cmsReq)
            throws EBaseException {
        HttpServletRequest httpReq = cmsReq.getHttpReq();
        HttpServletResponse httpResp = cmsReq.getHttpResp();

        IAuthToken authToken = authenticate(cmsReq);
        AuthzToken authzToken = null;

        try {
            authzToken = authorize(mAclMethod, authToken,
                        mAuthzResourceName, "read");
        } catch (EAuthzAccessDenied e) {
            logger.warn(CMS.getLogMessage("ADMIN_SRVLT_AUTH_FAILURE", e.toString()), e);

        } catch (Exception e) {
            logger.warn(CMS.getLogMessage("ADMIN_SRVLT_AUTH_FAILURE", e.toString()), e);
        }

        if (authzToken == null) {
            cmsReq.setStatus(CMSRequest.UNAUTHORIZED);
            return;
        }

        if (!(mAuthority instanceof IOCSPService)) {
            logger.warn(CMS.getLogMessage("CMSGW_CA_FROM_RA_NOT_IMP"));
            cmsReq.setError(new ECMSGWException(
                    CMS.getUserMessage("CMS_GW_NOT_YET_IMPLEMENTED")));
            cmsReq.setStatus(CMSRequest.ERROR);
            return;
        }

        CMSTemplate form = null;
        Locale[] locale = new Locale[1];

        try {
            form = getTemplate(mFormPath, httpReq, locale);
        } catch (IOException e) {
            logger.warn(CMS.getLogMessage("CMSGW_ERR_GET_TEMPLATE", mFormPath, e.toString()), e);
            cmsReq.setError(new ECMSGWException(
                    CMS.getUserMessage("CMS_GW_DISPLAY_TEMPLATE_ERROR")));
            cmsReq.setStatus(CMSRequest.ERROR);
            return;
        }

        ArgBlock header = new ArgBlock();
        ArgBlock fixed = new ArgBlock();
        CMSTemplateParams argSet = new CMSTemplateParams(header, fixed);

        IOCSPService ca = (IOCSPService) mAuthority;

        header.addLongValue("numReq", ca.getNumOCSPRequest());
        header.addLongValue("totalSec", ca.getOCSPRequestTotalTime());
        header.addLongValue("totalSignSec", ca.getOCSPTotalSignTime());
        header.addLongValue("totalLookupSec", ca.getOCSPTotalLookupTime());
        header.addLongValue("totalData", ca.getOCSPTotalData());
        long secs = 0;

        if (ca.getOCSPRequestTotalTime() != 0) {
            secs = (ca.getNumOCSPRequest() * 1000) / ca.getOCSPRequestTotalTime();
        }

        header.addLongValue("ReqSec", secs);

        try {
            ServletOutputStream out = httpResp.getOutputStream();

            httpResp.setContentType("text/html");
            form.renderOutput(out, argSet);
            cmsReq.setStatus(CMSRequest.SUCCESS);

        } catch (IOException e) {
            logger.warn(CMS.getLogMessage("CMSGW_ERR_STREAM_TEMPLATE", e.toString()), e);
            cmsReq.setError(new ECMSGWException(
                    CMS.getUserMessage("CMS_GW_DISPLAY_TEMPLATE_ERROR")));
            cmsReq.setStatus(CMSRequest.ERROR);
        }

        cmsReq.setStatus(CMSRequest.SUCCESS);
    }
}
