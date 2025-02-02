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
package com.netscape.certsrv.request;

//import java.io.Serializable;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import org.mozilla.jss.netscape.security.x509.CertificateExtensions;
import org.mozilla.jss.netscape.security.x509.CertificateSubjectName;
import org.mozilla.jss.netscape.security.x509.RevokedCertImpl;
import org.mozilla.jss.netscape.security.x509.X509CertImpl;
import org.mozilla.jss.netscape.security.x509.X509CertInfo;

import com.netscape.certsrv.authentication.IAuthToken;
import com.netscape.certsrv.base.IAttrSet;

/**
 * An interface that defines abilities of request objects,
 *
 * @version $Revision$, $Date$
 */
public interface IRequest extends Serializable {

    /**
     * Gets the primary identifier for this request.
     *
     * @return request id
     */
    RequestId getRequestId();

    /**
     * Gets the current state of this request.
     *
     * @return request status
     */
    RequestStatus getRequestStatus();

    /**
     * Gets the "sourceId" for the request. The sourceId is
     * assigned by the originator of the request (for example,
     * the EE servlet or the RA servlet.
     * <p>
     * The sourceId should be unique so that it can be used to retrieve request later without knowing the locally
     * assigned primary id (RequestID)
     * <p>
     *
     * @return
     *         the sourceId value (or null if none has been set)
     */
    public String getSourceId();

    /**
     * Sets the "sourceId" for this request. The request must be updated
     * in the database for this change to take effect. This can be done
     * by calling ARequestQueue.update() or by performing one of the
     * other operations like processRequest or approveRequest.
     *
     * @param id source id for this request
     */
    public void setSourceId(String id);

    /**
     * Gets the current owner of this request.
     *
     * @return request owner
     */
    public String getRequestOwner();

    /**
     * Sets the current owner of this request.
     *
     * @param owner
     *            The new owner of this request. If this value is set to null
     *            there will be no current owner
     */
    public void setRequestOwner(String owner);

    /**
     * Gets the type of this request.
     *
     * @return request type
     */
    public String getRequestType();

    /**
     * Sets the type or this request.
     *
     * @param type request type
     */
    public void setRequestType(String type);

    /**
     * Gets the version of this request.
     *
     * @return request version
     */
    public String getRequestVersion();

    /**
     * Gets the time this request was created.
     *
     * @return request creation time
     */
    Date getCreationTime();

    void setCreationTime(Date date);

    /**
     * Gets the time this request was last modified (defined
     * as updated in the queue) (See ARequestQueue.update)
     *
     * @return request last modification time
     */
    Date getModificationTime();

    void setModificationTime(Date date);

    /**
     * Copies meta attributes (excluding request Id, etc.) of another request
     * to this request.
     *
     * @param req another request
     */
    public void copyContents(IRequest req);

    /**
     * Gets context of this request.
     *
     * @return request context
     */
    public String getContext();

    /**
     * Sets context of this request.
     *
     * @param ctx request context
     */
    public void setContext(String ctx);

    /**
     * Sets status of this request.
     *
     * @param s request status
     */
    public void setRequestStatus(RequestStatus s);

    /**
     * Gets status of connector transfer.
     *
     * @return status of connector transfer
     */
    public boolean isSuccess();

    /**
     * Gets localized error message from connector transfer.
     *
     * @param locale request locale
     * @return error message from connector transfer
     */
    public String getError(Locale locale);

    /**
     * Get error code
     * @param locale request locale
     * @return error code
     */
    public String getErrorCode(Locale locale);

    /**************************************************************
     * ExtData data methods:
     *
     * These methods should be used in place of the mAttrData methods
     * deprecated above.
     *
     * These methods all store Strings in LDAP. This means they can no longer
     * be used as a garbage dump for all sorts of objects. A limited number
     * of helper methods are provided for Vectors/Arrays/Hashtables but the
     * keys and values for all of these should be Strings.
     *
     * The keys are used in the LDAP attribute names, and so much obey LDAP
     * key syntax rules: A-Za-z0-9 and hyphen.
     */

    /**
     * Sets an Extended Data string-key string-value pair.
     * All keys are lower cased because LDAP does not preserve case.
     *
     * @param key The extended data key
     * @param value The extended data value
     * @return false if key is invalid.
     */
    public boolean setExtData(String key, String value);

    /**
     * Sets an Extended Data string-key string-value pair.
     * The key and hashtable keys are all lowercased because LDAP does not
     * preserve case.
     *
     * @param key The extended data key
     * @param value The extended data value
     *            the Hashtable contains an illegal key.
     * @return false if the key or hashtable keys are invalid
     */
    public boolean setExtData(String key, Hashtable<String, String> value);

    /**
     * Checks whether the key is storing a simple String value, or a complex
     * (Vector/hashtable) structure.
     *
     * @param key The key to check for.
     * @return True if the key maps to a string. False if it maps to a
     *         hashtable.
     */
    public boolean isSimpleExtDataValue(String key);

    /**
     * Returns the String value stored for the String key. Returns null
     * if not found. Throws exception if key stores a complex data structure
     * (Vector/Hashtable).
     *
     * @param key The key to lookup (case-insensitive)
     * @return The value associated with the key. null if not found or if the
     *         key is associated with a non-string value.
     */
    public String getExtDataInString(String key);

    /**
     * Returns the Hashtable value for the String key. Returns null if not
     * found. Throws exception if the key stores a String value.
     *
     * The Hashtable returned is actually a subclass of Hashtable that
     * lowercases all keys used to access the hashtable. Its purpose is to
     * to make lookups seemless, but be aware it is not a normal hashtable and
     * might behave strangely in some cases (e.g., iterating keys)
     *
     * @param key The key to lookup (case-insensitive)
     * @return The hashtable value associated with the key. null if not found
     *         or if the key is associated with a string-value.
     */
    public Hashtable<String, String> getExtDataInHashtable(String key);

    /**
     * Returns all the keys stored in ExtData
     *
     * @return Enumeration of all the keys.
     */
    public Enumeration<String> getExtDataKeys();

    /**
     * Stores an array of Strings in ExtData.
     * The indices of the array are used as subkeys.
     *
     * @param key the ExtData key
     * @param values the array of string values to store
     * @return False if the key is invalid
     */
    public boolean setExtData(String key, String[] values);

    /**
     * Retrieves an array of Strings stored with the key.
     * This only works if the data was stored as an array. If the data
     * is not correct, this method will return null.
     *
     * @param key The ExtData key
     * @return The value. Null if not found or the data isn't an array.
     */
    public String[] getExtDataInStringArray(String key);

    /**
     * Removes the value of an extdata attribute.
     *
     * @param type key to delete
     */
    void deleteExtData(String type);

    /*****************************
     * Helper methods for ExtData
     ****************************/

    /**
     * Helper method to add subkey/value pair to a ExtData hashtable.
     * If the hashtable it exists, the subkey/value are added to it. Otherwise
     * a new hashtable is created.
     *
     * The key and subkey are lowercased because LDAP does not preserve case.
     *
     * @param key The top level key
     * @param subkey The hashtable data key
     * @param value The hashtable value
     * @return False if the key or subkey are invalid
     */
    public boolean setExtData(String key, String subkey, String value);

    /**
     * Helper method to retrieve an individual value from a Hashtable value.
     *
     * @param key the ExtData key
     * @param subkey the key in the Hashtable value (case insensitive)
     * @return the value corresponding to the key/subkey
     */
    public String getExtDataInString(String key, String subkey);

    /**
     * Helper method to store an Integer value. It converts the integer value
     * to a String and stores it.
     *
     * @param key the ExtData key
     * @param value the Integer to store (as a String)
     * @return False if the key or value are invalid
     */
    public boolean setExtData(String key, Integer value);

    /**
     * Retrieves an integer value. Returns null if not found or
     * the value can't be represented as an Integer.
     *
     * @param key The ExtData key to lookup
     * @return The integer value or null if not possible.
     */
    public Integer getExtDataInInteger(String key);

    /**
     * Stores an array of Integers
     *
     * @param key The extdata key
     * @param values The array of Integers to store
     * @return false if the key is invalid
     */
    public boolean setExtData(String key, Integer[] values);

    /**
     * Retrieves an array of Integers
     *
     * @param key The extdata key
     * @return The array of Integers or null on error.
     */
    public Integer[] getExtDataInIntegerArray(String key);

    /**
     * Helper method to store a BigInteger value. It converts the integer value
     * to a String and stores it.
     *
     * @param key the ExtData key
     * @param value the BigInteger to store (as a String)
     * @return False if the key or value are invalid
     */
    public boolean setExtData(String key, BigInteger value);

    /**
     * Retrieves a BigInteger value. Returns null if not found or
     * the value can't be represented as a BigInteger.
     *
     * @param key The ExtData key to lookup
     * @return The integer value or null if not possible.
     */
    public BigInteger getExtDataInBigInteger(String key);

    /**
     * Stores an array of BigIntegers
     *
     * @param key The extdata key
     * @param values The array of BigIntegers to store
     * @return false if the key is invalid
     */
    public boolean setExtData(String key, BigInteger[] values);

    /**
     * Retrieves an array of BigIntegers
     *
     * @param key The extdata key
     * @return The array of BigIntegers or null on error.
     */
    public BigInteger[] getExtDataInBigIntegerArray(String key);

    /**
     * Helper method to store an exception.
     * It actually stores the e.toString() value.
     *
     * @param key The ExtData key to store under
     * @param e The throwable to store
     * @return False if the key is invalid.
     */
    public boolean setExtData(String key, Throwable e);

    /**
     * Stores a byte array as base64 encoded text
     *
     * @param key The ExtData key
     * @param data The byte array to store
     * @return False if the key is invalid.
     */
    public boolean setExtData(String key, byte[] data);

    /**
     * Retrieves the data, which should be base64 encoded as a byte array.
     *
     * @param key The ExtData key
     * @return The data, or null if an error occurs.
     */
    public byte[] getExtDataInByteArray(String key);

    /**
     * Stores a X509CertImpl as base64 encoded text using the getEncode()
     * method.
     *
     * @param key The ExtData key
     * @param data certificate
     * @return False if the key is invalid.
     */
    public boolean setExtData(String key, X509CertImpl data);

    /**
     * Retrieves the data, which should be base64 encoded as a byte array.
     *
     * @param key The ExtData key
     * @return The data, or null if an error occurs.
     */
    public X509CertImpl getExtDataInCert(String key);

    /**
     * Stores an array of X509CertImpls as a base64 encoded text.
     *
     * @param key The ExtData key
     * @param data The array of certs to store
     * @return False if the key or data is invalid.
     */
    public boolean setExtData(String key, X509CertImpl[] data);

    /**
     * Retrieves an array of X509CertImpl.
     *
     * @param key The ExtData key
     * @return Array of certs, or null if not found or invalid data.
     */
    public X509CertImpl[] getExtDataInCertArray(String key);

    /**
     * Stores a X509CertInfo as base64 encoded text using the getEncodedInfo()
     * method.
     *
     * @param key The ExtData key
     * @param data certificate
     * @return False if the key is invalid.
     */
    public boolean setExtData(String key, X509CertInfo data);

    /**
     * Retrieves the data, which should be base64 encoded as a byte array.
     *
     * @param key The ExtData key
     * @return The data, or null if an error occurs.
     */
    public X509CertInfo getExtDataInCertInfo(String key);

    /**
     * Stores an array of X509CertInfos as a base64 encoded text.
     *
     * @param key The ExtData key
     * @param data The array of cert infos to store
     * @return False if the key or data is invalid.
     */
    public boolean setExtData(String key, X509CertInfo[] data);

    /**
     * Retrieves an array of X509CertInfo.
     *
     * @param key The ExtData key
     * @return Array of cert infos, or null if not found or invalid data.
     */
    public X509CertInfo[] getExtDataInCertInfoArray(String key);

    /**
     * Stores an array of RevokedCertImpls as a base64 encoded text.
     *
     * @param key The ExtData key
     * @param data The array of cert infos to store
     * @return False if the key or data is invalid.
     */
    public boolean setExtData(String key, RevokedCertImpl[] data);

    /**
     * Retrieves an array of RevokedCertImpl.
     *
     * @param key The ExtData key
     * @return Array of cert infos, or null if not found or invalid data.
     */
    public RevokedCertImpl[] getExtDataInRevokedCertArray(String key);

    /**
     * Stores the contents of the String Vector in ExtData.
     * TODO - as soon as we're allowed to use JDK5 this should be changed
     * to use Vector<String> data.
     *
     * Note that modifications to the Vector are not automatically reflected
     * after it is stored. You must call set() again to make the changes.
     *
     * @param key The extdata key to store
     * @param data A vector of Strings to store
     * @return False on key error or invalid data.
     */
    public boolean setExtData(String key, Vector<?> data);

    /**
     * Returns a vector of strings for the key.
     * Note that the returned vector, if modified, does not make changes
     * in ExtData. You must call setExtData() to propogate changes back
     * into ExtData.
     *
     * @param key The extdata key
     * @return A Vector of strings, or null on error.
     */
    public Vector<String> getExtDataInStringVector(String key);

    /**
     * Gets boolean value for given type or default value
     * if attribute is absent.
     *
     * @param type attribute type
     * @param defVal default attribute value
     * @return attribute value
     */
    boolean getExtDataInBoolean(String type, boolean defVal);

    /**
     * Gets extdata boolean value for given type or default value
     * if attribute is absent for this request with this prefix.
     *
     * @param prefix request prefix
     * @param type attribute type
     * @param defVal default attribute value
     * @return attribute value
     */
    public boolean getExtDataInBoolean(String prefix, String type, boolean defVal);

    /**
     * Stores an AuthToken the same as a Hashtable.
     *
     * @param key The ExtData key
     * @param data The authtoken to store
     * @return False if the key or data is invalid.
     */
    public boolean setExtData(String key, IAuthToken data);

    /**
     * Retrieves an authtoken.
     *
     * @param key The ExtData key
     * @return AuthToken, or null if not found or invalid data.
     */
    public IAuthToken getExtDataInAuthToken(String key);

    /**
     * Stores a CertificateExtensions in extdata.
     *
     * @param key The ExtData key
     * @param data The CertificateExtensions to store
     * @return False if the key or data is invalid.
     */
    public boolean setExtData(String key, CertificateExtensions data);

    /**
     * Retrieves the CertificateExtensions associated with the key.
     *
     * @param key The ExtData key
     * @return the object, or null if not found or invalid data.
     */
    public CertificateExtensions getExtDataInCertExts(String key);

    /**
     * Stores a CertificateSubjectName in extdata.
     *
     * @param key The ExtData key
     * @param data The CertificateSubjectName to store
     * @return False if the key or data is invalid.
     */
    public boolean setExtData(String key, CertificateSubjectName data);

    /**
     * Retrieves the CertificateSubjectName associated with the key.
     *
     * @param key The ExtData key
     * @return the object, or null if not found or invalid data.
     */
    public CertificateSubjectName getExtDataInCertSubjectName(String key);

    /**
     * @return IAttrSet wrapper with basic "get" functionality.
     */
    public IAttrSet asIAttrSet();

    /**
     * Get realm
     * @return String
     */
    public String getRealm();

    /**
     * Set the realm
     * @param realm
     */
    public void setRealm(String realm);
}
