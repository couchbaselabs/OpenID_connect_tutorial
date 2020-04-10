package syncgateway.rest.calls;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "_rev", "_exp" })
public class DocumentBody {
	public String _id;
	public String _rev;
	public String _exp;
	public Revisions _revisions;
	public Attachments _attachments;

	public String channels;
	public double price;
	public String name;
	public String type;

	public static class Revisions {
		public int start;
		public int end;
		public List<String> ids;
	}

	public static class Attachments {
		public AttachmentName attachment_name;
	}

	public static class AttachmentName {
		public String content_type;
	}

	public DocumentBody() {
		// TODO Auto-generated constructor stub
	}
}
