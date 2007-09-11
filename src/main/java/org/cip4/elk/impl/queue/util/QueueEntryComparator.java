/*
 * Created on Aug 29, 2004
 */
package org.cip4.elk.impl.queue.util;

import java.util.Comparator;

import org.cip4.elk.util.JDFDateComparator;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.util.JDFDate;

/**
 * A <code>Comparator</code> that orders queue entries in the sequence they
 * will be or have been executed:
 * <ol>
 * <li>Running entries</li>
 * <li>Suspended</li>
 * <ol>
 * <li>Youngest suspend time</li>
 * </ol>
 * <li>Waiting entries</li>
 * <ol>
 * <li>Highest priority (100 is the highest, 0 is the lowest)</li>
 * </ol>
 * <li>Held entries</li>
 * <ol>
 * <li>Youngest held time</li>
 * </ol>
 * <li>Completed & aborted entries</li>
 * <ol>
 * <li>Latest finished</li>
 * </ol>
 * </ol>
 * 
 * NOTE: if both entries has status Completed, Aborted, Held or Suspended they
 * sorted using the
 * {@link QueueEntryComparator#compareEndTimes(JDFQueueEntry, JDFQueueEntry)}
 * method.
 * 
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, 5.6 Queue Support </a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, Table 5-102 </a>
 * @see <a
 *      href="http://www.cip4.org/documents/jdf_specifications/JDF1.2.pdf">JDF
 *      Specification Release 1.2, Table 5-103 </a>
 * @author Claes Buckwalter (clabu@itn.liu.se)
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: QueueEntryComparator.java,v 1.3 2005/06/29 06:41:35 ola.stering Exp $
 */
public class QueueEntryComparator implements Comparator {

    public static final int SORT_ASCENDING = 1;
    public static final int SORT_DESCENDING = -1;

    private int _sortOrder;
    private JDFDateComparator _dateComparator;

    /**
     * Creates a queue entry sorter that uses ascending sort order. Priority 100
     * comes before 0.
     */
    public QueueEntryComparator() {
        this(true);
    }

    /**
     * Creates a queue entry sorter with the specified sort order.
     * 
     * @param sortOrder <code>true</code> for descending order;
     *            <code>false</code> for ascending order
     */
    public QueueEntryComparator(boolean sortAscending) {
        _dateComparator = new JDFDateComparator(sortAscending);
        if (sortAscending)
            _sortOrder = SORT_ASCENDING;
        else
            _sortOrder = SORT_DESCENDING;
    }

    /**
     * Compares two QueueEntries for order.
     * 
     * Copied from java.util.Comparator: Compares its two arguments for order.
     * Returns a negative integer, zero, or a positive integer as the first
     * argument is less than, equal to, or greater than the second.
     * 
     * The implementor must ensure that sgn(compare(x, y)) == -sgn(compare(y,
     * x)) for all x and y. (This implies that compare(x, y) must throw an
     * exception if and only if compare(y, x) throws an exception.)
     * 
     * The implementor must also ensure that the relation is transitive:
     * ((compare(x, y)>0) && (compare(y, z)>0)) implies compare(x, z)>0.
     * 
     * Finally, the implementer must ensure that compare(x, y)==0 implies that
     * sgn(compare(x, z))==sgn(compare(y, z)) for all z.
     * 
     * It is generally the case, but not strictly required that (compare(x,
     * y)==0) == (x.equals(y)). Generally speaking, any comparator that violates
     * this condition should clearly indicate this fact. The recommended
     * language is "Note: this comparator imposes orderings that are
     * inconsistent with equals."
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     * @throws IllegalArgumentException if o1 or o2 is not of type JDFQueueEntry
     */
    public int compare(Object o1, Object o2) {
        if (!(o1 instanceof JDFQueueEntry) || !(o2 instanceof JDFQueueEntry)) {
            String msg = "Objects in this comparator must be of type JDFQueueEntry";
            throw new IllegalArgumentException(msg);
        }
        JDFQueueEntry qe1 = (JDFQueueEntry) o1;
        JDFQueueEntry qe2 = (JDFQueueEntry) o2;
        // Order entries by status
        Integer status1 = new Integer(getSortValue(qe1.getQueueEntryStatus()));
        Integer status2 = new Integer(getSortValue(qe2.getQueueEntryStatus()));
        int result = status1.compareTo(status2);
        // Order entries with the same status
        if (result == 0) {
            // Sort by priority
            if (status1.intValue() == getSortValue(JDFQueueEntry.EnumQueueEntryStatus.Waiting)) {
                Integer prio1 = new Integer(qe1.getPriority());
                Integer prio2 = new Integer(qe2.getPriority());
                result = prio1.compareTo(prio2);
                // Sort by submission time
                if (result == 0) {
                    result = _dateComparator.compare(qe1.getSubmissionTime(),
                        qe2.getSubmissionTime());
                }
            } else if (status1.intValue() == getSortValue(JDFQueueEntry.EnumQueueEntryStatus.Suspended)) {
                // TODO Youngest suspend time fist
                result = compareEndTimes(qe1, qe2);
            } else if (status1.intValue() == getSortValue(JDFQueueEntry.EnumQueueEntryStatus.Held)) {
                // TODO Youngest held time first
                result = compareEndTimes(qe1, qe2);
            } else if (status1.intValue() == getSortValue(JDFQueueEntry.EnumQueueEntryStatus.Completed)) {
                // TODO Latest completed (end time) first, then submission time
                result = compareEndTimes(qe1, qe2);
            } else if (status1.intValue() == getSortValue(JDFQueueEntry.EnumQueueEntryStatus.Aborted)) {
                // TODO Latest aborted first
                result = compareEndTimes(qe1, qe2);
            }
        }
        return result * _sortOrder;
    }

    /**
     * Compares the <em>EndTime</em> of the <em>QueueEntries</em>. Positive
     * integer if (and the JDFDateComparator is sorted in ascending order):
     * <ul>
     * <li><em>qe1/@EndTime</em> is earlier than <em>qe2/@EndTime</em>
     * </li>
     * <li><em>qe1/@EndTime</em> is set but not <em>qe2/@EndTime</em></li>
     * <li>qe1 and qe2 <em>EndTime</em> is not set, but
     * <em>qe1/@SubmissionTime</em> is earlier than
     * <em>qe2/@SubmissionTime</em>.</li>
     * <li>qe1 and qe2 <em>EndTime</em> is not set and
     * <em>qe1/@SubmissionTime</em> is set but not
     * <em>qe2/@SubmissionTime</em>.</li>
     * </ul>
     * The same applies for negative integers but with qe1 and qe2 in changed
     * positions.
     * <p>
     * The method returns 0 if <em>qe1/EndTime</em> is equal to
     * <em>qe2/EndTime</em> or if <em>EndTimes</em> are not given and
     * <em>qe1/SubmissionTime</em> equals qe1 and qe2
     * <em>qe1/@SubmissionTime</em> is earlier than
     * <em>qe2/@SubmissionTime</em>.
     * </p>
     * 
     * @param qe1 <em>QueueEntry</em> to be compared.
     * @param qe2 <em>QueueEntry</em> to be compared.
     * @return positive integer if qe1 has younger <em>EndTime</em> than qe2,
     *         0 if the <em>EndTimes</em> are equal, negative otherwise.
     * @throws NullPointerException if qe1 or qe2 is <code>null</code>, or if
     *             both qe1 and qe2 has none of <em>EndTime</em> and
     *             <em>SubmissionTime</em> set.
     */
    public int compareEndTimes(JDFQueueEntry qe1, JDFQueueEntry qe2) {
        JDFDate d1 = qe1.getEndTime();
        JDFDate d2 = qe2.getEndTime();

        if (d1 == null && d2 == null) {
            d1 = qe1.getSubmissionTime();
            d2 = qe2.getSubmissionTime();
        }

        if (d1 == null && d2 != null) {
            return -1;
        } else if (d2 == null && d1 != null) {
            return 1;
        }
        return _dateComparator.compare(d1, d2);
    }

    /**
     * Returns an integer value for the specified
     * <code>EnumQueueEntryStatus</code> object that can be used for ordering
     * a QueueEntry. Currently, the values of
     * <code>EnumQueueEntryStatus.getValue</code> does not correspond to the
     * sort order of queue entries.
     * 
     * @param status
     * @return
     */
    private int getSortValue(JDFQueueEntry.EnumQueueEntryStatus status) {
        int value = -1;
        if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Running)) {
            value = 100;
        } else if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Suspended)) {
            value = 90;
        } else if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Waiting)) {
            value = 80;
        } else if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Held)) {
            value = 70;
        } else if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Completed)) {
            value = 60;
        } else if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Aborted)) {
            value = 50;
        } else if (status.equals(JDFQueueEntry.EnumQueueEntryStatus.Removed)) {
            value = 40;
        }
        return value;
    }
}
