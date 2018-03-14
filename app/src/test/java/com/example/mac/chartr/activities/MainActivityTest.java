package com.example.mac.chartr.activities;

import com.example.mac.chartr.CommonDependencyProvider;
import com.example.mac.chartr.R;
import com.example.mac.chartr.activities.PostTripActivity;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


import static org.mockito.Mockito.mock;

/**
 * Created by Michael Rush on 3/4/2018.
 *
 */

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
    private CommonDependencyProvider provider;

    @Before
    public void setup() {
        provider = mock(CommonDependencyProvider.class);
    }

    /**
     * PENDING ACTIVATION:  Currently, Mocking the main activity without a prior login
     *                      breaks assumptions about the precondition to the MainActivity.
     *                      We will update this method as we find solutions to that problem.
     */

    @Test
    public void stub() {
        Assert.assertTrue(true);
    }

}
