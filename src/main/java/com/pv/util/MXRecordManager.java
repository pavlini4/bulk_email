package com.pv.util;

import org.apache.commons.lang3.StringUtils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by sanitizer on 6/13/2016.
 *
 */
public class MXRecordManager{

    private final String dnsLookupPropertyKey = "java.naming.factory.initial";
    private final String dnsLookupPropertyValue = "com.sun.jndi.dns.DnsContextFactory";

    public MXRecordManager(){}

    public Map<Integer, String> getMXRecords(String emailAddress) throws NamingException{

        String domain = isAddressValid(emailAddress);
        if(domain == null){
            return new HashMap<>();
        }

        // Perform a DNS lookup for MX records in the domain
        Hashtable env = new Hashtable();
        env.put(dnsLookupPropertyKey, dnsLookupPropertyValue);

        DirContext ictx = new InitialDirContext(env);

        Attributes attrs = ictx.getAttributes(domain, new String[] {"MX"});
        Attribute attr = attrs.get("MX");

        // if we don't have an MX record, try the machine itself
        if (( attr == null ) || ( attr.size() == 0 )) {
            attrs = ictx.getAttributes(domain, new String[] {"A"});
            attr = attrs.get("A");

            if(attr == null){
                throw new NamingException("No match for name '" + domain + "'");
            }
        }

        // Huzzah! we have machines to try. Return them as an array list
        // NOTE: We SHOULD take the preference into account to be absolutely
        //   correct. This is left as an exercise for anyone who cares.
        Map<Integer, String> res = new HashMap<>();
        NamingEnumeration en = attr.getAll();

        while(en.hasMore()){
            String x = (String)en.next();
            String f[] = x.split(" ");

            if (f[1].endsWith(".")){
                f[1] = StringUtils.chop(f[1]);
            }

            res.put(Integer.parseInt(f[0]), f[1]);
        }

        return res;
    }

    public String isAddressValid(String address) {
        // Find the separator for the domain name
        int pos = address.indexOf('@');

        // If the address does not contain an '@', it's not valid
        if(pos == -1){
            return null;
        }

        return address.substring(++pos);
    }

}