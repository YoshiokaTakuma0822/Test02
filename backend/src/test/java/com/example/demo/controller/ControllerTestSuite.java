package com.example.demo.controller;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test suite that runs all controller tests.
 * This provides an easy way to run all controller tests together.
 */
@Suite
@SuiteDisplayName("Controller Test Suite")
@SelectClasses({
        HelloControllerTest.class,
        UserControllerTest.class,
        ChatControllerTest.class,
        ChatControllerWebSocketTest.class
})
public class ControllerTestSuite {
    // This class is just a placeholder for the Suite annotation
}
