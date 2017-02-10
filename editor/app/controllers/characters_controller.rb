class CharactersController < ApplicationController
  before_action :set_character, only: [:show, :edit, :update, :destroy]

  def index
    render json: Character.all
  end

  def show
    render json: @character
  end

  def create
    render json: Character.create!(character_params)
  end

  def update
    @character.update!(character_params)
    render json: @character
  end

  def destroy
    @character.destroy!
    render json: @character
  end

  private

  def set_character
    @character = Character.find(params[:id])
  end

  def character_params
    params.require(:character).permit(:name)
  end
end
