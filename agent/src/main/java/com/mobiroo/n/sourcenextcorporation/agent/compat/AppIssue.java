package com.mobiroo.n.sourcenextcorporation.agent.compat;

public class AppIssue {
	public enum AppIssueSeverity {
		INFO, WARNING, ERROR
	}
	
	protected String mId;
	protected String mPackageName;
	protected int mExplanation;
	protected int mTitle;
	protected int mLink;
	protected AppIssueSeverity mSeverity;	
	
	public int getLink() {
		return mLink;
	}

	public void setLink(int mLink) {
		this.mLink = mLink;
	}

	public String getId() {
		return mId;
	}

	public void setId(String mId) {
		this.mId = mId;
	}
	
	public AppIssueSeverity getSeverity() {
		return mSeverity;
	}

	public void setSeverity(AppIssueSeverity mSeverity) {
		this.mSeverity = mSeverity;
	}

	public int getTitle() {
		return mTitle;
	}

	public void setTitle(int mTitle) {
		this.mTitle = mTitle;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public void setPackageName(String mPackageName) {
		this.mPackageName = mPackageName;
	}

	public int getExplanation() {
		return mExplanation;
	}

	public void setExplanation(int mExplanation) {
		this.mExplanation = mExplanation;
	}
	
	public AppIssue(String id, String packageName, AppIssueSeverity severity, int title, int explanation, int link) {
		setId(id);
		setPackageName(packageName);
		setSeverity(severity);
		setTitle(title);
		setExplanation(explanation);
		setLink(link);
	}
}
