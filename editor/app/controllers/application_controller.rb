class ApplicationController < ActionController::Base
  protect_from_forgery with: :exception
  rescue_from ActiveRecord::RecordInvalid, with: :render_validation_error

  private

  def render_validation_error e
    full_message = "#{e.record.class.model_name.human} #{e.record.id}: #{e.record.errors.full_messages.join(", ")}"
    errors = camelize_hash_keys(e.record.errors.messages)
    render status: :unprocessable_entity, json: { errors: errors, full_error_message: full_message }
  end

  def camelize_hash_keys(hash)
    hash.to_h.deep_transform_keys { |key| key.to_s.camelize(:lower) }
  end
end
