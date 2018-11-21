package io.dinject.generator;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest {

  @Test
  public void commonParent() {

    assertEquals(Util.commonParent(null, "org.b"), "org.b");
    assertEquals(Util.commonParent("org.a", null), "org.a");

    assertEquals(Util.commonParent("org.foo.web", "org.foo.web.other"), "org.foo.web");
    assertEquals(Util.commonParent("org.foo.web", "org.foo.web.other.more"), "org.foo.web");

    assertEquals(Util.commonParent("org.foo.web", "org.foo.service"), "org.foo");
    assertEquals(Util.commonParent("org.foo.web.foo", "org.foo.service.blah"), "org.foo");

  }
}
