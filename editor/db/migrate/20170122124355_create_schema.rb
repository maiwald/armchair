class CreateSchema < ActiveRecord::Migration[5.0]
  def change
    create_table :characters do |t|
      t.string :name
      t.timestamps
    end

    create_table :dialogues do |t|
      t.string :label
      t.integer :initial_line_id
    end

    create_table :lines do |t|
      t.references :character, foreign_key: true
      t.text :text, null: false
      t.integer :next_item_id
      t.string :next_item_type
    end

    create_table :options do |t|
      4.times do |index|
        t.integer :"option_#{index}_next_line_id"
        t.text :"option_#{index}_text"
      end
    end
  end
end

