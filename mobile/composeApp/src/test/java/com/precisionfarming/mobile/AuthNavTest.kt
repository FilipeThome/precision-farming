package com.precisionfarming.mobile

import com.precisionfarming.mobile.ui.AuthNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthNavTest {
    @Test
    fun startRouteUsesHomeOnlyWhenSessionPresent() {
        assertEquals(AuthNav.HOME, AuthNav.startRoute("tok", "user-1"))
        assertEquals(AuthNav.LOGIN, AuthNav.startRoute(null, "user-1"))
        assertEquals(AuthNav.LOGIN, AuthNav.startRoute("tok", null))
        assertEquals(AuthNav.LOGIN, AuthNav.startRoute("", "user-1"))
        assertEquals(AuthNav.LOGIN, AuthNav.startRoute("tok", " "))
    }

    @Test
    fun shouldReplaceSkipsNullAndSameRoute() {
        assertFalse(AuthNav.shouldReplace(null, AuthNav.LOGIN))
        assertFalse(AuthNav.shouldReplace(AuthNav.LOGIN, AuthNav.LOGIN))
        assertTrue(AuthNav.shouldReplace(AuthNav.HOME, AuthNav.LOGIN))
        assertTrue(AuthNav.shouldReplace(AuthNav.LOGIN, AuthNav.HOME))
    }
}
