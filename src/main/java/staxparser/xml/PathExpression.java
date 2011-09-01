package staxparser.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

/**
 * <b>Simple XPath-like expressions for selecting XML content.</b>
 * 
 * <p>
 * / Absolute path. Selects any node that matches the path exactly
 * <p>
 * // Relative path. Selects any descendant of the root that matches
 * <p>
 * @ Selects attributes
 * 
 * <p>
 * Examples :<br>
 * 
 * "/"<br>
 * "//foo" <br>
 * "//foo/bar" <br>
 * "/foo" <br>
 * "/foo/bar/car"<br>
 * "/foo/bar/car@attr"<br>
 * "//{namespace}foo" <br>
 * "//{http://com.example/order}foo/bar"<br>
 * "/{http://com.example/order}foo/bar"<br>
 * "/{http://com.example/order}foo/{http://com.example/orderitem}bar"<br>
 * 
 * @author David Turanski
 * 
 */
public class PathExpression {
	/*
	 *  Using possessive quantifier on nested \w quantifier to avoid catastrophic backtrack.
	 *  Avoids giving back matched chars on backtrack when no match.
	 *  See http://www.regular-expressions.info/catastrophic.html
	 */
    private static final String patternStr = "^/|(/{1,2})((\\{([\\w:/.])+\\})?\\w++(/?))*(\\{([\\w:/.])+\\})?\\w+(@\\w+)?$";

    private static final Pattern pattern = Pattern.compile(patternStr);
    
    private static final Pattern validChars = Pattern.compile("[\\w:\\.\\{\\}\\/@]+");
  

    private final String selector;

    private final List<QName> qnames;

    private final boolean relative;

    /**
     * 
     * @param selector
     *            A valid selector. If no prefix is provided, it will be treated
     *            as a relative path.
     */
    public PathExpression(String selector) {
        if (selector == null) {
            throw new IllegalArgumentException("constructor argument cannot be null");
        }
        selector = selector.trim();
        /**
         * If no prefix provided, assume an element name. Create a relative
         * PathExpression
         */
        if (!selector.startsWith("/") && !selector.startsWith("//")) {
            selector = "//" + selector;
        }

        validateSelector(selector);
        this.selector = selector;
        qnames = parse(selector);

        relative = selector.startsWith("//");
    }

    /**
     * Creates a relative or absolute PathExpression
     * 
     * @param qnames
     *            A list of {@link javax.xml.namespace.QName}. The
     *            PathExpression will be built from the list.
     * @param relative
     *            Indicates whether the expression should be relative or
     *            absolute
     */
    public PathExpression(List<QName> qnames, boolean relative) {
        this.qnames = qnames;
        this.relative = relative;
        this.selector = buildSelector();
    }

    /**
     * Creates an absolute PathExpression
     * 
     * @param qnames
     *            A list of {@link javax.xml.namespace.QName}. The
     *            PathExpression will be built from the list.
     */
    public PathExpression(List<QName> qnames) {
        this.qnames = qnames;
        this.relative = false;
        this.selector = buildSelector();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof PathExpression)) {
            return false;
        }

        PathExpression otherPx = (PathExpression) other;
        return selector.equals(otherPx.selector);
    }

    @Override
    public int hashCode() {
        return selector.hashCode();
    }

    @Override
    /**
     * Returns the constructor argument (Path selector as a String)
     */
    public String toString() {
        return selector;
    }

    /**
     * <p>
     * Creates a new instance with the QName appended to the current path.
     * <p>
     * <code>
     * /* result is "/foo/bar" *&#47;<br> 
     * new PathExpression("/foo").push("bar) 
     * </code>
     * </p>
     * 
     * @param qname
     *            The node to append
     * @see javax.xml.namespace.QName
     * @return the new instance
     */
    public PathExpression push(QName qname) {
        List<QName> qnames = new ArrayList<QName>(this.getQNames());
        qnames.add(qname);
        return new PathExpression(qnames, this.isRelative());
    }

    /**
     * <p>
     * Creates a new instance removing the last node from the path
     * <p>
     * <code>
     * /* result is "/foo" *&#47;<br> 
     * new PathExpression("/foo/bar").pop() 
     * </code>
     * </p>
     * <p>
     * <code>
     * /* result is "/" *&#47; <br>
     * new PathExpression("/foo").pop()
     * </code>
     * </p>
     * <p>
     * <code>
     *  * /* result is null *&#47; <br>
     * new PathExpression("/").pop()
     * </code>
     * </p>
     * 
     * @return the new instance
     */
    public PathExpression pop() {
        if (selector.equals("/") || selector.equals("//")) {
            return null;
        }

        List<QName> qnames = new ArrayList<QName>(this.getQNames());
        qnames.remove(qnames.get(qnames.size() - 1));

        boolean relative = (qnames.size() == 0 ? false : this.isRelative());
        return new PathExpression(qnames, relative);
    }

    /**
     * Returns <code>true</code> if the expression matches the argument. Matches
     * on local name if the namespaceURI is not explicit.
     * <p>
     * Examples:
     * <p>
     * The following are true:
     * <p>
     * <code>
     * new PathExpression("//{http://com.example/order}foo/{http://com.example/orderitem}bar").matches(new PathExpression("//foo/bar"))<br>
     * new PathExpression("//{http://com.example/order}foo/bar").matches(new PathExpression("//foo/bar"))<br>
     * new PathExpression("/foo/bar").matches(new PathExpression("//foo/bar"))<br>
     * </code>
     * </p>
     * <p>
     * The following are false:
     * <p>
     * <code>
     * new PathExpression("//{http://com.example/order}foo/{http://com.example/orderitem}bar").matches(new PathExpression("//{http://com.acme/order}foo/{http://com.example/orderitem}bar"))<br>
     * </code>
     * </p>
     * 
     * @param expression
     * @return
     */
    public boolean matches(PathExpression expression) {

        if (!(this.relative || expression.relative) && qnames.size() != expression.qnames.size()) {
            return false;
        }

        boolean match = true;

        List<QName> qnames1 = new ArrayList<QName>(this.qnames);
        List<QName> qnames2 = new ArrayList<QName>(expression.qnames);

        while (match && qnames1.size() > 0 && qnames2.size() > 0) {
            int last1 = qnames1.size() - 1;
            int last2 = qnames2.size() - 1;
            QName qn1 = qnames1.get(last1);
            QName qn2 = qnames2.get(last2);

            match = qn1.equals(qn2);
            /**
             * No namespace matches any namespace
             */
            if (!match) {
                if (qn1.getLocalPart().equals(qn2.getLocalPart())) {
                    match = (qn1.getNamespaceURI().length() == 0 || qn2.getNamespaceURI().length() == 0);
                }
            }

            /**
             * if match keep going
             */
            if (match) {
                qnames1.remove(last1);
                qnames2.remove(last2);
            }
        }
        // }
        return match;
    }

    /**
     * Convert a selector to a List&lt;QName&gt;
     * 
     * @param selector
     * @return - QName list
     */
    static List<QName> parse(String selector) {
        // {http://com.example/order}foo/{http://com.example/orderitem}bar
        if (selector == null) {
            throw new IllegalArgumentException("constructor argument cannot be null");
        }
        selector = selector.trim();
        validateSelector(selector);
        List<QName> qnames = new ArrayList<QName>();
        while (selector.length() > 0) {
            while (selector.startsWith("/")) {
                selector = selector.replaceFirst("/", "");
            }

            int last = selector.indexOf('/');
            
            if (last < 0) {
                last = selector.length();
            }
            
            if (selector.startsWith("{")) {
                int closeBrace = selector.indexOf('}');
                int remaining = selector.substring(closeBrace).indexOf('/');
                if (remaining > 0) {
                    last = closeBrace + remaining;
                } else {
                    last = selector.length();
                }
            }
            String qnameStr = selector.substring(0, last);
            if (qnameStr.length() > 0) {
                qnames.add(QName.valueOf(qnameStr));
            }
            selector = selector.substring(last);
        }
        return qnames;
    }

    /*
     * Package scope
     */
    List<QName> getQNames() {
        return qnames;
    }
    
    static void validateSelector(String selector) {
    	/*
    	 * Got bit by  http://www.regular-expressions.info/catastrophic.html
    	 * First do a basic character validation
    	 */
    	if (!validChars.matcher(selector).matches()) {
            throw new IllegalArgumentException(selector + " is not a valid path expression");
        }
    	
    	/*
    	 * Now check the format 
    	 */
    	if (!pattern.matcher(selector).matches()) {
            throw new IllegalArgumentException(selector + " is not a valid path expression");
        }
    }

    boolean isRelative() {
        return relative;
    }

    /*
	 * 
	 */
    private String buildSelector() {
        StringBuilder sb = new StringBuilder();

        sb.append(relative ? "//" : "/");

        int i = 0;
        for (QName qname : qnames) {
            sb.append(qname.toString());
            if (i++ < qnames.size() - 1) {
                sb.append("/");
            }
        }

        return sb.toString();
    }

}
