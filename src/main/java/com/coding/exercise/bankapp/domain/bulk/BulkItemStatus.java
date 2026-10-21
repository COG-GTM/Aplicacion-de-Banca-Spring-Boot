package com.coding.exercise.bankapp.domain.bulk;

public enum BulkItemStatus {

	CREATED(true),
	FOUND(true),
	UPDATED(true),
	DELETED(true),
	DUPLICATE(false),
	INVALID(false),
	NOT_FOUND(false),
	NOT_OWNED(false),
	REJECTED(false);

	private final boolean success;

	BulkItemStatus(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}
}
