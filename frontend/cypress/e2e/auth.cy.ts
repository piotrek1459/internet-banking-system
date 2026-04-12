describe('auth flow', () => {
  it('shows login page', () => {
    cy.visit('/login')
    cy.contains('Secure sign in')
  })
})
