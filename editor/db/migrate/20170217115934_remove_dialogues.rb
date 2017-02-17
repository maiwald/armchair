class RemoveDialogues < ActiveRecord::Migration[5.0]
  def up
    drop_table :dialogues
  end
end
