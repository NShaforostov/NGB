package com.epam.catgenome.security.saml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import com.epam.catgenome.security.UserContext;

@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);
    private static final String ATTRIBUTES_DELIMITER = "=";

    @Value("${saml.authorities.attribute.names: null}")
    private List<String> authorities;

    @Value(
        "#{catgenome['saml.user.attributes'] != null ? catgenome['saml.user.attributes'].split(',') : new String[0]}")
    private Set<String> samlAttributes;

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        String userName = credential.getNameID().getValue().toUpperCase();
        List<String> groups = readAuthorities(credential);
        Map<String, String> attributes = readAttributes(credential);

        UserContext userContext = new UserContext(userName);
        userContext.setGroups(groups);
        userContext.setAttributes(attributes);
        userContext.setUserId(userName);

        return userContext;
    }

    private List<String> readAuthorities(SAMLCredential credential) {
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyList();
        }
        List<String> grantedAuthorities = new ArrayList<>();
        authorities.stream().forEach(auth -> {
            if (StringUtils.isEmpty(auth)) {
                return;
            }
            String[] attributeValues = credential.getAttributeAsStringArray(auth);
            if (attributeValues != null && attributeValues.length > 0) {
                grantedAuthorities.addAll(
                    Arrays.stream(attributeValues)
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()));
            }
        });
        return grantedAuthorities;
    }

    private Map<String, String> readAttributes(SAMLCredential credential) {
        if (CollectionUtils.isEmpty(samlAttributes)) {
            return Collections.emptyMap();
        }
        Map<String, String> parsedAttributes = new HashMap<>();
        for (String attribute : samlAttributes) {
            if (attribute.contains(ATTRIBUTES_DELIMITER)) {
                String[] splittedRecord = attribute.split(ATTRIBUTES_DELIMITER);
                String key = splittedRecord[0];
                String value = splittedRecord[1];
                if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
                    LOGGER.error("Can not parse saml user attributes property.");
                    continue;
                }
                String attributeValues = credential.getAttributeAsString(value);
                if (StringUtils.isNotEmpty(attributeValues)) {
                    parsedAttributes.put(key, attributeValues);
                }
            }
        }
        return parsedAttributes;
    }
}