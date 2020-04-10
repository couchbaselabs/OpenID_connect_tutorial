package syncgateway.rest.calls;

import java.util.ArrayList;
import java.util.List;

public class DocsArray {

	public List<DocumentBody> docs = new ArrayList<DocumentBody>();
	public boolean new_edits = false;

	public boolean addDocumentBody(DocumentBody docBody) {
		return docs.add(docBody);
	}

}
