package com.bizosys.onelineserver.sendmail;

import com.bizosys.onelineserver.service.Task;

public class MailTask implements Task {
	static int serialCounter = 0;
	
	public SmtpMsg msg;
	public String jobName;
	
	public MailTask(SmtpMsg msg) {
		jobName = "mail" + new Integer(++serialCounter).toString();
		this.msg = msg;
	}

	public String getJobName() {
		return jobName;
	}

	public void process() {
		MailSender.getInstance().send(msg);
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
}
