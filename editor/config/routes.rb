Rails.application.routes.draw do
  resources :characters
  root to: 'home#index'
end
