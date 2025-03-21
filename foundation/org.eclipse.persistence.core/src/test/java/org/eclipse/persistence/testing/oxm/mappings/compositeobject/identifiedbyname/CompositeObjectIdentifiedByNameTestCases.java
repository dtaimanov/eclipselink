/*
 * Copyright (c) 1998, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
package org.eclipse.persistence.testing.oxm.mappings.compositeobject.identifiedbyname;

import org.eclipse.persistence.testing.oxm.mappings.XMLWithJSONMappingTestCases;
import org.eclipse.persistence.testing.oxm.mappings.compositeobject.Employee;
import org.eclipse.persistence.testing.oxm.mappings.compositeobject.EmailAddress;
import org.eclipse.persistence.testing.oxm.mappings.compositeobject.MailingAddress;

public class CompositeObjectIdentifiedByNameTestCases extends XMLWithJSONMappingTestCases {

  private final static String XML_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/compositeobject/identifiedbyname/CompositeObjectIdentifiedByName.xml";
  private final static String JSON_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/compositeobject/identifiedbyname/CompositeObjectIdentifiedByName.json";
  private final static int CONTROL_EMPLOYEE_ID = 123;
  private final static String CONTROL_EMAIL_ADDRESS_USER_ID = "jane.doe";
  private final static String CONTROL_EMAIL_ADDRESS_DOMAIN = "example.com";
  private final static String CONTROL_MAILING_ADDRESS_STREET = "1 Any Street";
  private final static String CONTROL_MAILING_ADDRESS_CITY = "Ottawa";
  private final static String CONTROL_MAILING_ADDRESS_PROVINCE = "Ontario";
  private final static String CONTROL_MAILING_ADDRESS_POSTAL_CODE = "A1B 2C3";

  public CompositeObjectIdentifiedByNameTestCases(String name) throws Exception {
    super(name);
    setControlDocument(XML_RESOURCE);
    setControlJSON(JSON_RESOURCE);
        setProject(new CompositeObjectIdentifiedByNameProject());
  }

  @Override
  protected Object getControlObject() {
    Employee employee = new Employee();
    employee.setID(CONTROL_EMPLOYEE_ID);

    EmailAddress emailAddress = new EmailAddress();
    emailAddress.setUserID(CONTROL_EMAIL_ADDRESS_USER_ID);
    emailAddress.setDomain(CONTROL_EMAIL_ADDRESS_DOMAIN);
    employee.setEmailAddress(emailAddress);

    MailingAddress mailingAddress = new MailingAddress();
    mailingAddress.setStreet(CONTROL_MAILING_ADDRESS_STREET);
    mailingAddress.setCity(CONTROL_MAILING_ADDRESS_CITY);
    mailingAddress.setProvince(CONTROL_MAILING_ADDRESS_PROVINCE);
    mailingAddress.setPostalCode(CONTROL_MAILING_ADDRESS_POSTAL_CODE);
    employee.setMailingAddress(mailingAddress);

    return employee;
  }

}
