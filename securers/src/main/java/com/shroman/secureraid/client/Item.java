package com.shroman.secureraid.client;

import com.shroman.secureraid.utils.Utils;

public class Item {
	public static class Builder {
		private Item item;

		public Builder() {
			setItem(new Item());
		}

		private Builder(Item item) {
			setItem(new Item(item));
		}

		public Builder setId(int id) {
			Utils.validateNotNegative(id, "id");
			item.id = id;
			return this;
		}

		public Builder setFileSize(int size) {
			Utils.validatePositive(size, "file size");
			item.fileSize = size;
			return this;
		}

		public Builder setStripesNumber(int stripesNumber) {
			Utils.validatePositive(stripesNumber, "stripes number");
			item.stripesNumber = stripesNumber;
			return this;
		}
		
		public Item build() {
			validate();
			return new Item(item);
		}

		private void validate() {
			Utils.validatePositive(item.stripesNumber, "stripes number");
			Utils.validatePositive(item.fileSize, "file size");
			Utils.validateNotNegative(item.id, "id");
		}

		private void setItem(Item item) {
			this.item = item;
		}
	}

	private int id = -1;
	private int stripesNumber = -1;
	private int fileSize = -1;

	private Item() {
	}

	private Item(Item other) {
		id = other.id;
		stripesNumber = other.stripesNumber;
		fileSize = other.fileSize;
	}

	public int getFileSize() {
		return fileSize;
	}

	public int getStripesNumber() {
		return stripesNumber;
	}

	public int getId() {
		return id;
	}

	public Builder getSelfBuilder() {
		return new Builder(this);
	}
}
