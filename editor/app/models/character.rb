class Character < ApplicationRecord
  validates_uniqueness_of :name
end
