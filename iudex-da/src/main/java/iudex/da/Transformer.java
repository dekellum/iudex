package iudex.da;

import com.gravitext.htmap.UniMap;

/**
  * Transform content and references during insert/update operations. Supports
  * late revisions based on database current state.
 */
public interface Transformer
{
    /**
     * Transform content to be inserted/updated.
     * @param updated content to be written
     * @param current from database copy or null if not found. Should not be
     * modified.
     * @return content to write/update
     */
    UniMap transformContent( UniMap updated, UniMap current );

    /**
     * Transform a reference to be inserted/updated.
     * @param updated reference to be written
     * @param current from database copy or null if not found. Should not
     * be modified.
     * @return content to write/update
     */
    UniMap transformReference( UniMap updated, UniMap current );
}
