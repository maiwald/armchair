class Dialogue < ApplicationRecord
  validates_uniqueness_of :label
  validates_presence_of :label
end
