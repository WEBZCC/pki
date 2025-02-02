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
package com.netscape.cms.profile.input;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;

import com.netscape.certsrv.base.IConfigStore;
import com.netscape.certsrv.profile.EProfileException;
import com.netscape.certsrv.property.Descriptor;
import com.netscape.certsrv.property.IDescriptor;
import com.netscape.cms.profile.common.Profile;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmscore.request.Request;

/**
 * This class implements the image
 * input that collects a picture.
 * <p>
 *
 * @version $Revision$, $Date$
 */
public class FileSigningInput extends EnrollInput {

    public static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileSigningInput.class);

    public static final String URL = "file_signing_url";
    public static final String TEXT = "file_signing_text";
    public static final String SIZE = "file_signing_size";
    public static final String DIGEST = "file_signing_digest";
    public static final String DIGEST_TYPE = "file_signing_digest_type";

    public FileSigningInput() {
        addValueName(URL);
        addValueName(TEXT);
    }

    /**
     * Initializes this default policy.
     */
    @Override
    public void init(Profile profile, IConfigStore config)
            throws EProfileException {
        super.init(profile, config);
    }

    /**
     * Retrieves the localizable name of this policy.
     */
    @Override
    public String getName(Locale locale) {
        return CMS.getUserMessage(locale, "CMS_PROFILE_INPUT_FILE_SIGNING_NAME");
    }

    /**
     * Retrieves the localizable description of this policy.
     */
    @Override
    public String getText(Locale locale) {
        return CMS.getUserMessage(locale, "CMS_PROFILE_INPUT_FILE_SIGNING_TEXT");
    }

    public String toHexString(byte data[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xff;
            if (v < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    /**
     * Populates the request with this policy default.
     */
    @Override
    public void populate(Map<String, String> ctx, Request request) throws Exception {

        request.setExtData(TEXT, ctx.get(TEXT));
        request.setExtData(URL, ctx.get(URL));
        request.setExtData(DIGEST_TYPE, "SHA256");

        try {
            // retrieve file and calculate the hash
            URL url = new URL(ctx.get(URL));
            URLConnection c = url.openConnection();
            c.setAllowUserInteraction(false);
            c.setDoInput(true);
            c.setDoOutput(false);
            c.setUseCaches(false);
            c.connect();
            int len = c.getContentLength();
            request.setExtData(SIZE, Integer.toString(len));
            BufferedInputStream is = new BufferedInputStream(c.getInputStream());
            byte data[] = new byte[len];
            is.read(data, 0, len);
            is.close();

            // calculate digest
            MessageDigest digester = MessageDigest.getInstance("SHA256");
            byte digest[] = digester.digest(data);
            request.setExtData(DIGEST, toHexString(digest));
        } catch (Exception e) {
            logger.error("FileSigningInput populate failure " + e.getMessage(), e);
            throw new EProfileException(
                    CMS.getUserMessage(getLocale(request),
                            "CMS_PROFILE_FILE_NOT_FOUND"));
        }
    }

    /**
     * Retrieves the descriptor of the given value
     * parameter by name.
     */
    @Override
    public IDescriptor getValueDescriptor(Locale locale, String name) {
        if (name.equals(URL)) {
            return new Descriptor(IDescriptor.STRING, null,
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_INPUT_FILE_SIGNING_URL"));
        } else if (name.equals(TEXT)) {
            return new Descriptor(IDescriptor.STRING, null,
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_INPUT_FILE_SIGNING_TEXT"));
        }
        return null;
    }
}
