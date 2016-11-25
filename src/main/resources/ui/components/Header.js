import React from 'react'

const Header = ({ title }) => (
  <nav className="navbar navbar-inverse">
    <div className="container-fluid">
      <div className="navbar-header" style={{letterSpacing: 6}}>
        <a className="navbar-brand" href="#">{title}</a>
      </div>
    </div>
  </nav>
)

export default Header
